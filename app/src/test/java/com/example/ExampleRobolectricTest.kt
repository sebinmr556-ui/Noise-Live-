package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.FeedRecommendationEngine
import com.example.data.SampleData
import com.example.model.Post
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NOICE LIVE", appName)
  }

  @Test
  fun `test personalized feed recommendation engine`() {
    val engine = FeedRecommendationEngine()
    val testPosts = listOf(
      Post(
        id = "test_1",
        userId = "user_elena",
        username = "elena_lens",
        userAvatar = "",
        imageUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb",
        caption = "Sunset in Yosemite",
        tags = listOf("photography", "landscape"),
        likesCount = 50,
        commentsCount = 10
      )
    )
    val ranked = engine.rankForYou(
      posts = testPosts,
      currentUser = SampleData.currentUser,
      followedUserIds = setOf("user_elena"),
      userLikedPostIds = setOf("test_1"),
      userBookmarkedPostIds = setOf("test_1")
    )
    assertTrue("Ranked feed should not be empty", ranked.isNotEmpty())
    assertNotNull("Recommendation reason should be computed", ranked.first().recommendationReason)
    assertTrue("Top match percentage should be high", (ranked.first().recommendationReason?.matchPercentage ?: 0) >= 80)
  }
}
