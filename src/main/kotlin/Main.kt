import net.mamoe.mirai.Bot
import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.BotConfiguration
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.auth.BotAuthorization
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.Image.Key.queryUrl

import net.mamoe.mirai.message.data.findIsInstance
import java.io.File


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.message.data.findIsInstance
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.MiraiLogger
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.launch
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.FileOutputStream

import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource

suspend fun main() {
    val qqId = 3111855223L
    val botDir = File("bots", qqId.toString())
    botDir.mkdirs()


    val bot = BotFactory.newBot(qqId, BotAuthorization.byQRCode()) {
        protocol = BotConfiguration.MiraiProtocol.MACOS
    }.alsoLogin()


    println("机器人登录成功！")

    GlobalEventChannel.subscribeAlways<FriendMessageEvent> { event ->
        val message = event.message.content
        val sender = event.sender.nick
        val image: Image? = event.message.findIsInstance<Image>()
        println("收到来自 $sender 的消息：$message")
        if (message == "ping") {
            event.sender.sendMessage("pong!")
        } else if (image != null) {

            event.subject.sendMessage(PlainText("收到图片，正在处理：").plus(image))
            val url = image.queryUrl()
            println(url)
            try {
                val file = download(url)
                event.subject.sendMessage("图片已保存到：${file.absolutePath}")
            } catch (e: Exception) {
                event.subject.sendMessage("图片保存失败：${e.message}")
            }

        }else if(message=="美图"){

            val file = File("C:/Users/PC/Pictures/美图/AAA8DD84CBF67EB7DB8EF547487DE758.png")
            if (!file.exists()) {
                event.sender.sendMessage("文件不存在")
                return@subscribeAlways
            }
            file.toExternalResource().use { res ->
                event.sender.sendImage(res)
            }
        }



    }

    bot.join()
}


suspend fun download(url: String, dir: String = "image"): File = withContext(Dispatchers.IO) {
    HttpClient(OkHttp).use { client ->
        val response = client.get(url)
        if (!response.status.isSuccess()) throw java.io.IOException("下载失败：${response.status}")

        val ext = response.headers["Content-Type"]?.let {
            when {
                it.contains("jpeg") -> "jpg"
                it.contains("png") -> "png"
                it.contains("gif") -> "gif"
                else -> "bin"
            }
        } ?: "jpg"

        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS").format(LocalDateTime.now())
        val file = File(dir, "$timestamp.$ext").absoluteFile.also { it.parentFile.mkdirs() }

        response.bodyAsChannel().copyTo(file.outputStream())
        file
    }
}


