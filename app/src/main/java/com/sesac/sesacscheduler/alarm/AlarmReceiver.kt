package com.sesac.sesacscheduler.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import com.sesac.sesacscheduler.R
import com.sesac.sesacscheduler.common.RetrofitManager
import com.sesac.sesacscheduler.common.TMAP_MARKET_URL
import com.sesac.sesacscheduler.common.TMAP_PACKAGE_NAME
import com.sesac.sesacscheduler.common.TMAP_ROUTE_URL
import com.sesac.sesacscheduler.common.logE
import com.sesac.sesacscheduler.tmap.TMapInfo
import com.sesac.sesacscheduler.weather.WeatherInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getIntExtra("scheduleId", -1)
        val scheduleTitle = intent.getStringExtra("scheduleTitle")
        val appointmentPlace = intent.getStringExtra("appointmentPlace")
        val destinationLatitude = intent.getDoubleExtra("latitude", 0.0)
        val destinationlongitude = intent.getDoubleExtra("longitude", 0.0)

        logE(
            "알람리시버 도착",
            "id: $scheduleId, 제목: $scheduleTitle, 장소: $appointmentPlace, 위도: $destinationLatitude, 경도: $destinationlongitude"
        )

        getCurrentLocation(context) { currentLatitude, currentLongitude ->
            logE("현재", "위도: $currentLatitude, 경도: $currentLongitude")

            CoroutineScope(Dispatchers.IO).launch {
                if (destinationLatitude != 0.0) {
                    logE("test","$destinationLatitude")
                    showRouteNotification(
                        context,
                        getWeatherInfo(destinationLatitude, destinationlongitude).await(),
                        getTMapInfo(currentLatitude, currentLongitude, destinationLatitude, destinationlongitude).await(),
                        scheduleTitle!!,
                        appointmentPlace!!,
                        currentLatitude, currentLongitude, destinationLatitude, destinationlongitude
                    )
                } else {
                    showNotification(
                        context,
                        getWeatherInfo(destinationLatitude, destinationlongitude).await(),
                        scheduleTitle!!
                    )
                }
            }
        }
    }

    private fun showNotification(context: Context, weather: WeatherInfo, title: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "schedule_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Schedule Notifications", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        // 특정 intent로 가기 - tmap을 넣어볼까?
//        val mainIntent = Intent(context, AlarmReceiver::class.java)
//        val pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("일정 알람 이지롱~")
            .setContentText(
                "일정: $title"
//                + "기온: ${weather.temperature}"
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            //.setContentIntent(pendingIntent) - 특정 intent로 가기
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)

    }

    private fun showRouteNotification(context: Context, weather: WeatherInfo, tmap: TMapInfo, title: String, place: String
    , currentLatitude: Double, currentLongitude: Double, destinationLatitude: Double, destinationLongitude: Double){
        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Notification Channel 아이디, 이름, 설명, 중요도 설정
        val channelId = "channel_one"
        val channelName = "첫 번째 채널"
        val channelDescription = "첫 번째 채널에 대한 설명입니다."
        val importance = NotificationManager.IMPORTANCE_DEFAULT

        // NotificationChannel 객체 생성
        val notificationChannel = NotificationChannel(channelId, channelName, importance)
        // 설명 설정
        notificationChannel.description = channelDescription
        // 채널에 대한 각종 설정(불빛, 진동 등)
        notificationChannel.enableLights(true)
        notificationChannel.lightColor = Color.RED
        notificationChannel.enableVibration(true)
        notificationChannel.vibrationPattern = longArrayOf(100L, 200L, 300L)
        // 시스템에 notificationChannel 등록
        notificationManager.createNotificationChannel(notificationChannel)

        // T맵이 설치되어 있는지 확인
        val isTmapInstalled = try {
            context.packageManager.getPackageInfo(TMAP_PACKAGE_NAME, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

        val tMapIntent = if(isTmapInstalled){
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(String.format(TMAP_ROUTE_URL, place, destinationLongitude, destinationLatitude, currentLongitude, currentLatitude))
                `package` = TMAP_PACKAGE_NAME
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse(String.format(TMAP_MARKET_URL, TMAP_ROUTE_URL)))
        }

        // PendingIntent 생성
        val pendingIntent = PendingIntent.getActivity(context, 0, tMapIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notificationCompatBuilder = NotificationCompat.Builder(context, channelId)

        notificationCompatBuilder.let {
            // 알림창 접혔을 때
            val customSmallNotificationView = RemoteViews("com.sesac.sesacscheduler", R.layout.notification_small)
                .apply {
                    setTextViewText(R.id.smallNotificationTitle, "일정")
                    setTextViewText(
                        R.id.smallNotificationInfo,
                        "기온: ${weather.temperature}˚C " +
                                "자동차: ${tmap.carTime/60}분 " +
                                "도보: ${tmap.walkTime/60}분 " +
                                "거리: ${tmap.distance}m"
                    )
                }
            // 알림창 폈을 때
            val customExpandedNotificationView = RemoteViews("com.sesac.sesacscheduler", R.layout.notification_expanded)
                .apply {
                    setTextViewText(R.id.textViewTemperature, "${weather.temperature}℃")
                    setTextViewText(R.id.textViewCarDistance, "${tmap.distance}m")
                    setTextViewText(R.id.textViewCarTime, "${tmap.carTime/60}분")
                    setTextViewText(R.id.textViewWalkDistance, "${tmap.distance}m")
                    setTextViewText(R.id.textViewWalkTime, "${tmap.walkTime/60}분")
                    when(weather.sky){
                        "1" -> setImageViewResource(R.id.skyImage, R.drawable.sky1)
                        "3" -> setImageViewResource(R.id.skyImage, R.drawable.sky3)
                        "4" -> setImageViewResource(R.id.skyImage, R.drawable.sky4)
                    }
                    setImageViewResource(R.id.carIcon, R.drawable.car_icon)
                    setImageViewResource(R.id.walkIcon, R.drawable.walk_icon)
                }

            // 커스텀뷰 셋팅
            it.setCustomContentView(customSmallNotificationView)
            it.setCustomBigContentView(customExpandedNotificationView)
            it.setCustomHeadsUpContentView(customSmallNotificationView)
            // 작은 아이콘 설정
            it.setSmallIcon(R.drawable.calendar_icon)
            // 시간 설정
            it.setWhen(System.currentTimeMillis())

            // 알림과 동시에 진동 설정(권한 필요(
            it.setDefaults(Notification.DEFAULT_VIBRATE)
            // 클릭 시 알림이 삭제되도록 설정
            it.setAutoCancel(true)
            it.setContentIntent(pendingIntent)
//            if(tMapIntent.resolveActivity(context.packageManager)!=null){
//                it.setContentIntent(pendingIntent)
//            } else{
//                val playStoreIntent = Intent(
//                    Intent.ACTION_VIEW,
//                    Uri.parse("market://details?id=com.skt.tmap.ku")
//                )
//                context.startActivity(playStoreIntent)
//            }
        }

        val notification = notificationCompatBuilder.build()
        // Notification 식별자 값, Notification 객체
        notificationManager.notify(0, notification)

        // overlay 실행
        Intent(context, AlarmOverlayService::class.java).also {
            it.putExtra("weather", weather)
            it.putExtra("tmap", tmap)
            // ScheduleInfo를 전달하는게 나을 것 같음
            it.putExtra("title", title)
            it.putExtra("place", place)
            context.startService(it)
        }
    }
    private fun getCurrentLocation(context: Context, callback: (Double, Double) -> Unit) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // 위치 권한 확인
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            logE("위치 권한이 필요합니다.", "")
            return
        }

        // 위치 정보 가져오기
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnCompleteListener { task: Task<Location> ->
            if (task.isSuccessful && task.result != null) {
                val location: Location = task.result
                val latitude = location.latitude
                val longitude = location.longitude
                // 위치 값을 콜백으로 전달
                callback(latitude, longitude)
            } else {
                logE("현재 위치 가져오기 실패",task.toString())
                callback(0.0, 0.0) // 위치를 가져오지 못한 경우 기본 값 전달
            }
        }
    }

    private fun getWeatherInfo(latitude: Double, longitude: Double): Deferred<WeatherInfo>
            = CoroutineScope(Dispatchers.IO).async {
        val result = RetrofitManager.WeatherService.weatherService.getWeatherForecast(
            nx = latitude.toInt(),
            ny = longitude.toInt()
        )
        result.let {
            if(it.isSuccessful){
                val firstItemSKY = it.body()?.response?.body?.items?.item?.filter { item ->
                    item.category == "SKY"
                }?.sortedBy { item ->
                    item.fcstTime.toInt()
                }?.first()

                val firstItemT1H = it.body()?.response?.body?.items?.item?.filter { item ->
                    item.category == "T1H"
                }?.sortedBy { item ->
                    item.fcstTime.toInt()
                }?.first()

                // 하늘상태(SKY) 코드 : 맑음(1), 구름많음(3), 흐림(4)
                WeatherInfo(firstItemSKY?.fcstValue?:"1", firstItemT1H?.fcstValue?:"2")
            } else {
                Log.e("getWeatherInfo", "실패 ${it.code()}")
                throw Exception("getWeatherInfo: 실패")
            }
        }
    }

    private fun getTMapInfo(currentLatitude: Double, currentLongitude: Double, latitude: Double, longitude: Double): Deferred<TMapInfo>
            = CoroutineScope(Dispatchers.IO).async{
        val carInfo = CoroutineScope(Dispatchers.IO).async {
            val result = RetrofitManager.TMapService.tMapService.getCarResult(
                startX = currentLongitude,
                startY = currentLatitude,
                endX = longitude,
                endY = latitude
            )
            result.let {
                if(it.isSuccessful){
                    val totalTime = it.body()?.features?.first()?.properties?.totalTime
                    val totalDistance = it.body()?.features?.first()?.properties?.totalDistance
                    logE("차량 정보","차량 걸리는 시간: $totalTime,차량 걸리는 거리: $totalDistance")
                    Pair(totalDistance!!, totalTime!!)
                }else{
                    Log.e("test1234", "실패 ${it.code()}")
                    throw Exception("getWeatherInfo: 실패")
                }
            }
        }
        val walkInfo = CoroutineScope(Dispatchers.IO).async {
            val result = RetrofitManager.TMapService.tMapService.getWalkResult(
                startX = currentLongitude,
                startY = currentLatitude,
                endX = longitude,
                endY = latitude,
                startName = "%EC%B6%9C%EB%B0%9C",
                endName = "%EB%B3%B8%EC%82%AC"
            )
            result.let {
                if(it.isSuccessful){
                    val totalDistance = it.body()?.features?.first()?.properties?.totalDistance
                    val totalTime = it.body()?.features?.first()?.properties?.totalTime
                    logE("도보정보","도보 걸리는 시간: $totalTime,도보 걸리는 거리: $totalDistance")
                    Pair(totalDistance!!, totalTime!!)
                }else{
                    Log.e("test1234", "실패 ${it.code()}")
                    throw Exception("getWeatherInfo: 실패")
                }
            }
        }
        //거리, 시간, 시간
        TMapInfo(carInfo.await().first, carInfo.await().second, walkInfo.await().second)
    }
}