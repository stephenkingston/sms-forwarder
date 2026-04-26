package com.smsforwarder.app.mail

import com.smsforwarder.app.data.ForwardingConfig
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import javax.mail.AuthenticationFailedException
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.SendFailedException
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

sealed class SendResult {
    data object Success : SendResult()
    data class Failure(val reason: String, val transient: Boolean) : SendResult()
}

object GmailSender {

    private const val SMTP_HOST = "smtp.gmail.com"
    private const val SMTP_PORT = "587"

    fun send(
        config: ForwardingConfig,
        smsSender: String,
        smsBody: String,
        smsReceivedAt: Long
    ): SendResult {
        val props = Properties().apply {
            put("mail.smtp.host", SMTP_HOST)
            put("mail.smtp.port", SMTP_PORT)
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.starttls.required", "true")
            put("mail.smtp.connectiontimeout", "20000")
            put("mail.smtp.timeout", "20000")
            put("mail.smtp.writetimeout", "20000")
            put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() =
                PasswordAuthentication(config.senderEmail, config.appPassword)
        })

        return try {
            val msg = MimeMessage(session).apply {
                setFrom(InternetAddress(config.senderEmail))
                setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(config.recipientEmail)
                )
                subject = "SMS from $smsSender"
                setText(buildBody(smsSender, smsBody, smsReceivedAt), "UTF-8")
                sentDate = Date()
            }
            Transport.send(msg)
            SendResult.Success
        } catch (e: AuthenticationFailedException) {
            SendResult.Failure(
                "Authentication failed — verify the Gmail app password is correct and 2FA is enabled.",
                transient = false
            )
        } catch (e: SendFailedException) {
            val text = e.message.orEmpty().lowercase()
            val transient = "quota" in text || "rate" in text || "try again" in text
            SendResult.Failure(
                e.message ?: "SMTP rejected the message.",
                transient = transient
            )
        } catch (e: MessagingException) {
            val cause = e.cause
            if (cause is IOException) {
                SendResult.Failure(
                    "Network error: ${cause.message ?: cause.javaClass.simpleName}",
                    transient = true
                )
            } else {
                SendResult.Failure(
                    e.message ?: "Mail server error.",
                    transient = true
                )
            }
        } catch (e: Exception) {
            SendResult.Failure(
                e.message ?: e.javaClass.simpleName,
                transient = true
            )
        }
    }

    fun sendTest(config: ForwardingConfig): SendResult = send(
        config = config,
        smsSender = "SMS Forwarder",
        smsBody = "This is a test email from the SMS Forwarder app. If you received this, your setup is working correctly.",
        smsReceivedAt = System.currentTimeMillis()
    )

    private fun buildBody(sender: String, body: String, receivedAt: Long): String {
        val ts = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
            .format(Date(receivedAt))
        return buildString {
            appendLine("From: $sender")
            appendLine("Received: $ts")
            appendLine()
            append(body)
        }
    }
}
