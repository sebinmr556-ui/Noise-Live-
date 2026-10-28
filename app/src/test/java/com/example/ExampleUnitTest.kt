package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testOnlinePresenceStateAndConversationActive() {
    val onlineConv = com.example.model.Conversation(
      id = "conv_1",
      participantId = "user_1",
      participantName = "Elena",
      participantUsername = "elena_v",
      participantAvatar = "",
      isOnline = true,
      presenceState = com.example.model.OnlinePresenceState.ONLINE
    )
    assertTrue("Should be active now when presence is ONLINE", onlineConv.isUserActiveNow)

    val offlineConv = com.example.model.Conversation(
      id = "conv_2",
      participantId = "user_2",
      participantName = "Zane",
      participantUsername = "zane_a",
      participantAvatar = "",
      isOnline = false,
      presenceState = com.example.model.OnlinePresenceState.OFFLINE
    )
    assertFalse("Should not be active when OFFLINE", offlineConv.isUserActiveNow)
  }

  @Test
  fun testVoiceNoteMessageCreation() {
    val voiceMsg = com.example.model.ChatMessage(
      id = "msg_1",
      conversationId = "conv_1",
      senderId = "user_1",
      senderName = "Elena",
      senderAvatar = "",
      text = "Voice message",
      isMine = false,
      messageType = com.example.model.MessageType.VOICE_NOTE,
      voiceDurationSec = 8,
      voiceAmplitudes = listOf(0.4f, 0.8f, 0.6f),
      audioFilePath = "/data/local/tmp/voice.m4a"
    )
    assertEquals(com.example.model.MessageType.VOICE_NOTE, voiceMsg.messageType)
    assertEquals(8, voiceMsg.voiceDurationSec)
    assertEquals(3, voiceMsg.voiceAmplitudes.size)
    assertNotNull(voiceMsg.audioFilePath)
  }
}
