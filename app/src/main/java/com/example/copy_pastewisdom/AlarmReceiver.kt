package com.example.copy_pastewisdom

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.core.app.NotificationCompat
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED") {
            // Reschedule on boot or permission grant
            if (QuoteRepository.isNotificationsEnabled(context)) {
                NotificationScheduler.scheduleDailyNotification(context)
            }
            return
        }

        val quotes = QuoteRepository.getQuotesFromCache(context)
        if (quotes.isNotEmpty()) {
            // Match app logic: Use day of year to select "Today's Wisdom"
            val dayOfYear = Calendar.getInstance()[Calendar.DAY_OF_YEAR]
            val item = quotes[dayOfYear % quotes.size]
            
            // We need a coroutine for Coil image loading
            CoroutineScope(Dispatchers.Main).launch {
                val portrait = fetchPortrait(context, item, quotes)
                showNotification(context, item, portrait)
                // Schedule for tomorrow
                NotificationScheduler.scheduleDailyNotification(context)
            }
        }
    }

    private suspend fun fetchPortrait(context: Context, item: QuoteItem, allQuotes: List<QuoteItem>): Bitmap? {
        val imageUrl = allQuotes.find { it.author == item.author && !it.imageUrl.isNullOrBlank() }?.imageUrl
            ?: return null
            
        return try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .build()
            val result = loader.execute(request)
            (result.drawable as? BitmapDrawable)?.bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun showNotification(context: Context, item: QuoteItem, portrait: Bitmap?) {
        val channelId = "daily_quote_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Daily Wisdom", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_wisdom)
            .setContentTitle("Today's Wisdom")
            .setContentText("From ${item.author}")
            .setStyle(NotificationCompat.BigTextStyle()
                .setBigContentTitle("Wisdom from ${item.author}")
                .bigText("“${item.quote}”")
                .setSummaryText("Daily Wisdom"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (portrait != null) {
            builder.setLargeIcon(portrait)
        }

        notificationManager.notify(1001, builder.build())
    }
}
