package com.example.fitnessclub

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

// ==================== МОДЕЛИ ДАННЫХ ====================
data class User(
    val id: String = UUID.randomUUID().toString(),
    val username: String,
    val passwordHash: String,
    val role: String,
    val name: String,
    val email: String = "",
    val phone: String = "",
    val membershipId: String? = null,
    val membershipPurchaseDate: String? = null,
    val membershipExpiryDate: String? = null,
    val membershipFrozen: Boolean = false,
    val membershipFrozenDate: String? = null,
    val membershipFrozenDays: Int = 0,
    val profilePhoto: String? = null,
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
    val isActive: Boolean = true
)

data class Trainer(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val name: String,
    val specialization: String = "Фитнес",
    val experience: Int = 0,
    val rating: Double = 0.0,
    val description: String = "",
    val photoUrl: String? = null
)

data class Workout(
    val id: String = UUID.randomUUID().toString(),
    val trainerId: String,
    val clientId: String? = null,
    val title: String,
    val description: String,
    val date: String,
    val time: String,
    val duration: Int = 60,
    val maxParticipants: Int = 1,
    var currentParticipants: Int = 0,
    var isAvailable: Boolean = true,
    val location: String = "Зал 1",
    val price: Int = 0,
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
    var isActive: Boolean = true
)

data class Booking(
    val id: String = UUID.randomUUID().toString(),
    val workoutId: String,
    val clientId: String,
    val bookingDate: String,
    var status: String = "confirmed",
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
)

data class Payment(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val amount: Int,
    val paymentDate: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
    val paymentMethod: String,
    val description: String,
    val status: String = "completed"
)

data class Membership(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val durationDays: Int,
    val price: Int,
    val workoutsCount: Int,
    val description: String,
    val isActive: Boolean = true
)

data class FreezeHistory(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val freezeDate: String,
    val unfreezeDate: String? = null,
    val daysFrozen: Int,
    val status: String = "frozen" // "frozen", "unfrozen"
)

data class Review(
    val id: String = UUID.randomUUID().toString(),
    val trainerId: String,
    val clientId: String,
    val clientName: String,
    val rating: Int,
    val comment: String,
    val date: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
)

data class WorkoutReview(
    val id: String = UUID.randomUUID().toString(),
    val workoutId: String,
    val clientId: String,
    val clientName: String,
    val rating: Int,
    val comment: String,
    val date: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
)

// ==================== КЛАСС ДЛЯ ХЭШИРОВАНИЯ ПАРОЛЕЙ ====================
object PasswordHasher {
    fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, hash: String): Boolean {
        return hashPassword(password) == hash
    }
}

// ==================== КЛАСС ДЛЯ ХРАНЕНИЯ ДАННЫХ ====================
object DataStorage {
    private const val PREFS_NAME = "FitnessClubPrefs"
    private const val USERS_KEY = "users"
    private const val TRAINERS_KEY = "trainers"
    private const val WORKOUTS_KEY = "workouts"
    private const val BOOKINGS_KEY = "bookings"
    private const val PAYMENTS_KEY = "payments"
    private const val MEMBERSHIPS_KEY = "memberships"
    private const val FREEZE_HISTORY_KEY = "freeze_history"
    private const val REVIEWS_KEY = "reviews"
    private const val WORKOUT_REVIEWS_KEY = "workout_reviews"

    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveUsers(users: List<User>) {
        val json = gson.toJson(users)
        sharedPreferences.edit().putString(USERS_KEY, json).apply()
    }

    fun loadUsers(): MutableList<User> {
        val json = sharedPreferences.getString(USERS_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<User>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }

    fun saveTrainers(trainers: List<Trainer>) {
        val json = gson.toJson(trainers)
        sharedPreferences.edit().putString(TRAINERS_KEY, json).apply()
    }

    fun loadTrainers(): MutableList<Trainer> {
        val json = sharedPreferences.getString(TRAINERS_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<Trainer>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }

    fun saveWorkouts(workouts: List<Workout>) {
        val json = gson.toJson(workouts)
        sharedPreferences.edit().putString(WORKOUTS_KEY, json).apply()
    }

    fun loadWorkouts(): MutableList<Workout> {
        val json = sharedPreferences.getString(WORKOUTS_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<Workout>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }

    fun saveBookings(bookings: List<Booking>) {
        val json = gson.toJson(bookings)
        sharedPreferences.edit().putString(BOOKINGS_KEY, json).apply()
    }

    fun loadBookings(): MutableList<Booking> {
        val json = sharedPreferences.getString(BOOKINGS_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<Booking>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }

    fun savePayments(payments: List<Payment>) {
        val json = gson.toJson(payments)
        sharedPreferences.edit().putString(PAYMENTS_KEY, json).apply()
    }

    fun loadPayments(): MutableList<Payment> {
        val json = sharedPreferences.getString(PAYMENTS_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<Payment>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }

    fun saveMemberships(memberships: List<Membership>) {
        val json = gson.toJson(memberships)
        sharedPreferences.edit().putString(MEMBERSHIPS_KEY, json).apply()
    }

    fun loadMemberships(): MutableList<Membership> {
        val json = sharedPreferences.getString(MEMBERSHIPS_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<Membership>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }

    fun saveFreezeHistory(history: List<FreezeHistory>) {
        val json = gson.toJson(history)
        sharedPreferences.edit().putString(FREEZE_HISTORY_KEY, json).apply()
    }

    fun loadFreezeHistory(): MutableList<FreezeHistory> {
        val json = sharedPreferences.getString(FREEZE_HISTORY_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<FreezeHistory>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }

    fun saveReviews(reviews: List<Review>) {
        val json = gson.toJson(reviews)
        sharedPreferences.edit().putString(REVIEWS_KEY, json).apply()
    }

    fun loadReviews(): MutableList<Review> {
        val json = sharedPreferences.getString(REVIEWS_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<Review>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }

    fun saveWorkoutReviews(reviews: List<WorkoutReview>) {
        val json = gson.toJson(reviews)
        sharedPreferences.edit().putString(WORKOUT_REVIEWS_KEY, json).apply()
    }

    fun loadWorkoutReviews(): MutableList<WorkoutReview> {
        val json = sharedPreferences.getString(WORKOUT_REVIEWS_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<WorkoutReview>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }
}

// ==================== КЛАСС ДЛЯ ПРОВЕРКИ ПРАВ ДОСТУПА ====================
object PermissionChecker {

    enum class Permission {
        VIEW_TRAINERS,
        ADD_TRAINER,
        EDIT_TRAINER,
        DELETE_TRAINER,
        CREATE_WORKOUT,
        EDIT_WORKOUT,
        DELETE_WORKOUT,
        VIEW_ALL_WORKOUTS,
        VIEW_STATISTICS,
        VIEW_ALL_USERS,
        BOOK_WORKOUT,
        CANCEL_BOOKING,
        VIEW_OWN_BOOKINGS,
        VIEW_OWN_WORKOUTS,
        VIEW_CLIENTS,
        VIEW_SCHEDULE,
        EDIT_OWN_PROFILE,
        MANAGE_MEMBERSHIPS,
        VIEW_PAYMENTS,
        VIEW_REPORTS,
        FREEZE_MEMBERSHIP
    }

    private val rolePermissions = mapOf(
        "admin" to setOf(
            Permission.VIEW_TRAINERS,
            Permission.ADD_TRAINER,
            Permission.EDIT_TRAINER,
            Permission.DELETE_TRAINER,
            Permission.VIEW_ALL_WORKOUTS,
            Permission.VIEW_STATISTICS,
            Permission.VIEW_ALL_USERS,
            Permission.EDIT_OWN_PROFILE,
            Permission.MANAGE_MEMBERSHIPS,
            Permission.VIEW_PAYMENTS,
            Permission.VIEW_REPORTS,
            Permission.FREEZE_MEMBERSHIP
        ),
        "trainer" to setOf(
            Permission.VIEW_TRAINERS,
            Permission.CREATE_WORKOUT,
            Permission.EDIT_WORKOUT,
            Permission.DELETE_WORKOUT,
            Permission.VIEW_OWN_WORKOUTS,
            Permission.VIEW_CLIENTS,
            Permission.VIEW_SCHEDULE,
            Permission.EDIT_OWN_PROFILE
        ),
        "client" to setOf(
            Permission.VIEW_TRAINERS,
            Permission.BOOK_WORKOUT,
            Permission.CANCEL_BOOKING,
            Permission.VIEW_OWN_BOOKINGS,
            Permission.EDIT_OWN_PROFILE,
            Permission.FREEZE_MEMBERSHIP
        )
    )

    fun hasPermission(user: User, permission: Permission): Boolean {
        return rolePermissions[user.role]?.contains(permission) ?: false
    }

    fun checkPermissionOrShowError(user: User, permission: Permission, context: android.content.Context): Boolean {
        val hasPermission = hasPermission(user, permission)
        if (!hasPermission) {
            Toast.makeText(context, "У вас нет прав для этого действия", Toast.LENGTH_SHORT).show()
        }
        return hasPermission
    }

    fun getRoleName(role: String): String {
        return when(role) {
            "admin" -> "Администратор"
            "trainer" -> "Тренер"
            "client" -> "Клиент"
            else -> "Неизвестно"
        }
    }
}

// ==================== КЛАСС ДЛЯ УВЕДОМЛЕНИЙ ====================
object NotificationHelper {
    const val CHANNEL_ID = "fitness_channel"
    const val CHANNEL_NAME = "Фитнес уведомления"
    const val NOTIFICATION_ID = 1

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомления о тренировках"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendNotification(context: Context, title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                showNotification(context, title, message)
            } else {
                Log.d("Notification", "Permission not granted for notifications")
            }
        } else {
            showNotification(context, title, message)
        }
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun scheduleWorkoutReminder(context: Context, workout: Workout, timeInMillis: Long) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("title", "Напоминание о тренировке")
                putExtra("message", "Сегодня в ${workout.time} тренировка: ${workout.title}")
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                workout.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        timeInMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// ==================== КЛАСС ДЛЯ ПЛАТЕЖЕЙ ====================
object PaymentProcessor {

    fun processPayment(userId: String, amount: Int, method: String, description: String): Payment {
        val payment = Payment(
            userId = userId,
            amount = amount,
            paymentMethod = method,
            description = description
        )
        FitnessDatabase.payments.add(payment)
        FitnessDatabase.saveAllData()
        Log.d("PAYMENT", "Платеж добавлен: $amount руб., метод: $method")
        return payment
    }

    fun getUserPayments(userId: String): List<Payment> {
        return FitnessDatabase.payments.filter { it.userId == userId }
    }

    fun getTotalSpent(userId: String): Int {
        return FitnessDatabase.payments.filter { it.userId == userId && it.status == "completed" }
            .sumOf { it.amount }
    }

    fun getAllPayments(): List<Payment> {
        return FitnessDatabase.payments.toList()
    }
}

// ==================== КЛАСС ДЛЯ ОТЧЕТОВ ====================
object ReportGenerator {

    data class ClubReport(
        val totalUsers: Int,
        val totalTrainers: Int,
        val totalClients: Int,
        val totalWorkouts: Int,
        val totalBookings: Int,
        val totalRevenue: Int,
        val totalFrozenMemberships: Int,
        val popularTrainers: List<Pair<String, Int>>,
        val popularWorkouts: List<Pair<String, Int>>,
        val popularWorkoutReviews: List<Pair<String, Double>>,
        val periodStart: String,
        val periodEnd: String
    )

    fun generateReport(startDate: String, endDate: String): ClubReport {
        val periodWorkouts = FitnessDatabase.workouts.filter {
            it.date >= startDate && it.date <= endDate && it.isActive
        }

        val periodBookings = FitnessDatabase.bookings.filter {
            it.bookingDate.substring(0, 10) >= startDate &&
                    it.bookingDate.substring(0, 10) <= endDate &&
                    it.status == "confirmed"
        }

        val periodPayments = FitnessDatabase.payments.filter {
            it.paymentDate.substring(0, 10) >= startDate &&
                    it.paymentDate.substring(0, 10) <= endDate &&
                    it.status == "completed"
        }

        val frozenMemberships = FitnessDatabase.users.count { it.membershipFrozen }

        val trainerBookingCount = mutableMapOf<String, Int>()
        periodBookings.forEach { booking ->
            val workout = FitnessDatabase.workouts.find { it.id == booking.workoutId }
            workout?.let {
                trainerBookingCount[it.trainerId] = trainerBookingCount.getOrDefault(it.trainerId, 0) + 1
            }
        }

        val popularTrainers = trainerBookingCount.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { entry ->
                val trainer = FitnessDatabase.trainers.find { it.id == entry.key }
                (trainer?.name ?: "Неизвестно") to entry.value
            }

        val workoutBookingCount = mutableMapOf<String, Int>()
        periodBookings.forEach { booking ->
            workoutBookingCount[booking.workoutId] = workoutBookingCount.getOrDefault(booking.workoutId, 0) + 1
        }

        val popularWorkouts = workoutBookingCount.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { entry ->
                val workout = FitnessDatabase.workouts.find { it.id == entry.key }
                (workout?.title ?: "Неизвестно") to entry.value
            }

        val workoutRatings = mutableMapOf<String, MutableList<Int>>()
        FitnessDatabase.workoutReviews.forEach { review ->
            if (!workoutRatings.containsKey(review.workoutId)) {
                workoutRatings[review.workoutId] = mutableListOf()
            }
            workoutRatings[review.workoutId]?.add(review.rating)
        }

        val avgWorkoutRatings = workoutRatings.mapValues { it.value.average() }
        val popularWorkoutReviews = avgWorkoutRatings.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { entry ->
                val workout = FitnessDatabase.workouts.find { it.id == entry.key }
                (workout?.title ?: "Неизвестно") to entry.value
            }

        return ClubReport(
            totalUsers = FitnessDatabase.users.size,
            totalTrainers = FitnessDatabase.trainers.size,
            totalClients = FitnessDatabase.users.count { it.role == "client" },
            totalWorkouts = periodWorkouts.size,
            totalBookings = periodBookings.size,
            totalRevenue = periodPayments.sumOf { it.amount },
            totalFrozenMemberships = frozenMemberships,
            popularTrainers = popularTrainers,
            popularWorkouts = popularWorkouts,
            popularWorkoutReviews = popularWorkoutReviews,
            periodStart = startDate,
            periodEnd = endDate
        )
    }
}

// ==================== КЛАСС ДЛЯ РАБОТЫ С ФОТО ====================
object PhotoHelper {

    fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }
}

// ==================== БАЗА ДАННЫХ ====================
object FitnessDatabase {
    lateinit var users: MutableList<User>
    lateinit var trainers: MutableList<Trainer>
    lateinit var workouts: MutableList<Workout>
    lateinit var bookings: MutableList<Booking>
    lateinit var payments: MutableList<Payment>
    lateinit var memberships: MutableList<Membership>
    lateinit var freezeHistory: MutableList<FreezeHistory>
    lateinit var reviews: MutableList<Review>
    lateinit var workoutReviews: MutableList<WorkoutReview>

    fun init(context: Context) {
        DataStorage.init(context)

        users = DataStorage.loadUsers()
        trainers = DataStorage.loadTrainers()
        workouts = DataStorage.loadWorkouts()
        bookings = DataStorage.loadBookings()
        payments = DataStorage.loadPayments()
        memberships = DataStorage.loadMemberships()
        freezeHistory = DataStorage.loadFreezeHistory()
        reviews = DataStorage.loadReviews()
        workoutReviews = DataStorage.loadWorkoutReviews()

        if (users.isEmpty()) {
            initializeData()
        } else {
            Log.d("DATABASE", "Загружено ${users.size} пользователей")
            Log.d("DATABASE", "Загружено ${bookings.size} бронирований")
            Log.d("DATABASE", "Загружено ${freezeHistory.size} записей о заморозке")
        }
    }

    private fun initializeData() {
        memberships.addAll(listOf(
            Membership(
                id = "m001",
                name = "Базовый",
                durationDays = 30,
                price = 3000,
                workoutsCount = 8,
                description = "8 тренировок в месяц"
            ),
            Membership(
                id = "m002",
                name = "Стандарт",
                durationDays = 30,
                price = 5000,
                workoutsCount = 12,
                description = "12 тренировок в месяц"
            ),
            Membership(
                id = "m003",
                name = "Премиум",
                durationDays = 30,
                price = 8000,
                workoutsCount = 20,
                description = "20 тренировок в месяц"
            )
        ))

        val admin = User(
            id = "admin_001",
            username = "admin",
            passwordHash = PasswordHasher.hashPassword("admin123"),
            role = "admin",
            name = "Александр Иванов",
            email = "admin@fitnessclub.ru",
            phone = "+7 (999) 123-45-67"
        )

        val trainer1 = User(
            id = "trainer_001",
            username = "trainer1",
            passwordHash = PasswordHasher.hashPassword("trainer123"),
            role = "trainer",
            name = "Иван Петров",
            email = "ivan@fitnessclub.ru",
            phone = "+7 (999) 234-56-78"
        )

        val trainer2 = User(
            id = "trainer_002",
            username = "trainer2",
            passwordHash = PasswordHasher.hashPassword("trainer123"),
            role = "trainer",
            name = "Мария Сидорова",
            email = "maria@fitnessclub.ru",
            phone = "+7 (999) 345-67-89"
        )

        val client1 = User(
            id = "client_001",
            username = "client1",
            passwordHash = PasswordHasher.hashPassword("client123"),
            role = "client",
            name = "Алексей Смирнов",
            email = "alex@email.ru",
            phone = "+7 (999) 456-78-90",
            membershipId = "m002",
            membershipPurchaseDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            membershipExpiryDate = getDateAfterDays(30)
        )

        users.addAll(listOf(admin, trainer1, trainer2, client1))

        trainers.addAll(listOf(
            Trainer(
                id = "t001",
                userId = trainer1.id,
                name = "Иван Петров",
                specialization = "Силовые тренировки, Бодибилдинг",
                experience = 8,
                rating = 4.9,
                description = "Сертифицированный тренер с 8-летним опытом."
            ),
            Trainer(
                id = "t002",
                userId = trainer2.id,
                name = "Мария Сидорова",
                specialization = "Йога, Пилатес, Стретчинг",
                experience = 6,
                rating = 4.8,
                description = "Инструктор групповых программ."
            )
        ))

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val tomorrow = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis() + 86400000))
        val nextWeek = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis() + 7 * 86400000))

        workouts.addAll(listOf(
            Workout(
                id = "w001",
                trainerId = "t001",
                title = "Силовая тренировка",
                description = "Проработка всех групп мышц",
                date = tomorrow,
                time = "18:00",
                duration = 90,
                maxParticipants = 10,
                currentParticipants = 7,
                location = "Зал тяжелой атлетики",
                price = 800
            ),
            Workout(
                id = "w002",
                trainerId = "t002",
                title = "Утренняя йога",
                description = "Йога для пробуждения",
                date = tomorrow,
                time = "07:00",
                duration = 75,
                maxParticipants = 20,
                currentParticipants = 15,
                location = "Зал групповых программ",
                price = 500
            ),
            Workout(
                id = "w003",
                trainerId = "t001",
                title = "Интенсивный кроссфит",
                description = "Высокоинтенсивная тренировка",
                date = nextWeek,
                time = "19:00",
                duration = 60,
                maxParticipants = 8,
                currentParticipants = 3,
                location = "Функциональный зал",
                price = 900
            )
        ))

        bookings.add(
            Booking(
                id = "b001",
                workoutId = "w001",
                clientId = client1.id,
                bookingDate = today,
                status = "confirmed"
            )
        )

        reviews.add(
            Review(
                id = "r001",
                trainerId = "t001",
                clientId = client1.id,
                clientName = client1.name,
                rating = 5,
                comment = "Отличный тренер! Очень помог с программой тренировок.",
                date = today
            )
        )

        workoutReviews.add(
            WorkoutReview(
                id = "wr001",
                workoutId = "w001",
                clientId = client1.id,
                clientName = client1.name,
                rating = 5,
                comment = "Отличная тренировка! Очень понравилась.",
                date = today
            )
        )

        payments.add(
            Payment(
                id = "p001",
                userId = client1.id,
                amount = 5000,
                paymentMethod = "card",
                description = "Абонемент Стандарт",
                status = "completed"
            )
        )

        saveAllData()
        Log.d("DATABASE", "Инициализированы начальные данные")
    }

    private fun getDateAfterDays(days: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, days)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }

    fun saveAllData() {
        DataStorage.saveUsers(users)
        DataStorage.saveTrainers(trainers)
        DataStorage.saveWorkouts(workouts)
        DataStorage.saveBookings(bookings)
        DataStorage.savePayments(payments)
        DataStorage.saveMemberships(memberships)
        DataStorage.saveFreezeHistory(freezeHistory)
        DataStorage.saveReviews(reviews)
        DataStorage.saveWorkoutReviews(workoutReviews)
        Log.d("DATABASE", "Все данные сохранены")
    }

    fun getUserByCredentials(username: String, password: String): User? {
        val user = users.find { it.username == username && it.isActive }
        if (user != null && PasswordHasher.verifyPassword(password, user.passwordHash)) {
            return user
        }
        return null
    }

    fun isUsernameAvailable(username: String): Boolean {
        return users.none { it.username == username }
    }

    fun getTrainerByUserId(userId: String): Trainer? {
        return trainers.find { it.userId == userId }
    }

    fun getWorkoutsByTrainer(trainerId: String): List<Workout> {
        return workouts.filter { it.trainerId == trainerId && it.isActive }
    }

    fun getAvailableWorkouts(): List<Workout> {
        return workouts.filter { it.isAvailable && it.currentParticipants < it.maxParticipants && it.isActive }
    }

    fun getUpcomingWorkouts(): List<Workout> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return workouts.filter { it.date >= today && it.isActive }.sortedBy { it.date }
    }

    fun getBookingsByClient(clientId: String): List<Booking> {
        val result = bookings.filter { it.clientId == clientId && it.status == "confirmed" }
        Log.d("BOOKINGS", "Клиент $clientId имеет ${result.size} записей")
        return result
    }

    fun getReviewsForTrainer(trainerId: String): List<Review> {
        return reviews.filter { it.trainerId == trainerId }
    }

    fun getAverageRating(trainerId: String): Double {
        val trainerReviews = reviews.filter { it.trainerId == trainerId }
        return if (trainerReviews.isNotEmpty()) {
            trainerReviews.map { it.rating }.average()
        } else 0.0
    }

    fun getWorkoutReviews(workoutId: String): List<WorkoutReview> {
        return workoutReviews.filter { it.workoutId == workoutId }
    }

    fun getAverageWorkoutRating(workoutId: String): Double {
        val workoutRev = workoutReviews.filter { it.workoutId == workoutId }
        return if (workoutRev.isNotEmpty()) {
            workoutRev.map { it.rating }.average()
        } else 0.0
    }

    fun bookWorkout(workoutId: String, clientId: String): Boolean {
        val workout = workouts.find { it.id == workoutId }
        return if (workout != null && workout.currentParticipants < workout.maxParticipants && workout.isActive) {
            workout.currentParticipants++
            if (workout.currentParticipants >= workout.maxParticipants) {
                workout.isAvailable = false
            }

            val today = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val booking = Booking(
                workoutId = workoutId,
                clientId = clientId,
                bookingDate = today
            )
            bookings.add(booking)

            saveAllData()

            Log.d("BOOKING", "Тренировка забронирована: $workoutId, клиент: $clientId")
            Log.d("BOOKING", "Всего бронирований: ${bookings.size}")

            true
        } else {
            false
        }
    }

    fun cancelBooking(bookingId: String) {
        val booking = bookings.find { it.id == bookingId }
        booking?.let {
            it.status = "cancelled"
            val workout = workouts.find { workout -> workout.id == it.workoutId }
            workout?.let { w ->
                w.currentParticipants--
                if (!w.isAvailable && w.currentParticipants < w.maxParticipants) {
                    w.isAvailable = true
                }
            }
            saveAllData()
            Log.d("BOOKING", "Бронирование отменено: $bookingId")
        }
    }

    fun cancelWorkout(workoutId: String) {
        val workout = workouts.find { it.id == workoutId }
        workout?.let {
            it.isActive = false
            bookings.filter { b -> b.workoutId == workoutId && b.status == "confirmed" }
                .forEach { b -> b.status = "cancelled" }
            saveAllData()
            Log.d("WORKOUT", "Тренировка отменена: $workoutId")
        }
    }

    fun updateTrainer(trainerId: String, name: String, specialization: String, experience: Int, description: String): Boolean {
        val trainer = trainers.find { it.id == trainerId }
        return if (trainer != null) {
            val index = trainers.indexOf(trainer)
            trainers[index] = trainer.copy(
                name = name,
                specialization = specialization,
                experience = experience,
                description = description
            )
            saveAllData()
            true
        } else {
            false
        }
    }

    fun updateWorkout(workoutId: String, title: String, description: String, date: String, time: String,
                      duration: Int, maxParticipants: Int, location: String, price: Int): Boolean {
        val workout = workouts.find { it.id == workoutId }
        return if (workout != null) {
            val index = workouts.indexOf(workout)
            workouts[index] = workout.copy(
                title = title,
                description = description,
                date = date,
                time = time,
                duration = duration,
                maxParticipants = maxParticipants,
                location = location,
                price = price
            )
            saveAllData()
            true
        } else {
            false
        }
    }

    fun updateUserProfile(userId: String, name: String, email: String, phone: String, photoBase64: String?): Boolean {
        val user = users.find { it.id == userId }
        return if (user != null) {
            val index = users.indexOf(user)
            users[index] = user.copy(
                name = name,
                email = email,
                phone = phone,
                profilePhoto = photoBase64 ?: user.profilePhoto
            )

            if (user.role == "trainer") {
                val trainer = trainers.find { it.userId == userId }
                trainer?.let {
                    val trainerIndex = trainers.indexOf(it)
                    trainers[trainerIndex] = it.copy(
                        name = name,
                        photoUrl = photoBase64
                    )
                }
            }

            saveAllData()
            true
        } else {
            false
        }
    }

    fun freezeMembership(userId: String): Boolean {
        val user = users.find { it.id == userId }
        if (user != null && user.membershipId != null && !user.membershipFrozen) {
            // Проверяем, не превышен ли лимит заморозки (15 дней за период)
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val frozenDaysThisYear = freezeHistory
                .filter { it.userId == userId && it.status == "frozen" }
                .filter { it.freezeDate.startsWith(currentYear.toString()) }
                .sumOf { it.daysFrozen }

            val remainingFreezeDays = 15 - frozenDaysThisYear

            if (remainingFreezeDays <= 0) {
                return false // Лимит заморозки исчерпан
            }

            val freezeDays = min(remainingFreezeDays, 15)

            val today = Date()
            val freezeDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(today)

            val index = users.indexOf(user)
            users[index] = user.copy(
                membershipFrozen = true,
                membershipFrozenDate = freezeDate,
                membershipFrozenDays = freezeDays
            )

            val freezeRecord = FreezeHistory(
                userId = userId,
                freezeDate = freezeDate,
                daysFrozen = freezeDays,
                status = "frozen"
            )
            freezeHistory.add(freezeRecord)

            saveAllData()
            Log.d("FREEZE", "Абонемент заморожен для пользователя $userId на $freezeDays дней")
            return true
        }
        return false
    }

    fun unfreezeMembership(userId: String): Boolean {
        val user = users.find { it.id == userId }
        if (user != null && user.membershipFrozen) {
            // Рассчитываем новую дату окончания абонемента
            val currentExpiryDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(user.membershipExpiryDate)
            val today = Date()

            val calendar = Calendar.getInstance()
            calendar.time = currentExpiryDate ?: today
            calendar.add(Calendar.DAY_OF_YEAR, user.membershipFrozenDays)

            val newExpiryDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            val index = users.indexOf(user)
            users[index] = user.copy(
                membershipFrozen = false,
                membershipFrozenDate = null,
                membershipFrozenDays = 0,
                membershipExpiryDate = newExpiryDate
            )

            // Обновляем запись в истории
            val freezeRecord = freezeHistory.find { it.userId == userId && it.status == "frozen" }
            freezeRecord?.let {
                val recordIndex = freezeHistory.indexOf(it)
                freezeHistory[recordIndex] = it.copy(
                    unfreezeDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    status = "unfrozen"
                )
            }

            saveAllData()
            Log.d("FREEZE", "Абонемент разморожен для пользователя $userId")
            return true
        }
        return false
    }

    fun getRemainingFreezeDays(userId: String): Int {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val frozenDaysThisYear = freezeHistory
            .filter { it.userId == userId && it.status == "frozen" }
            .filter { it.freezeDate.startsWith(currentYear.toString()) }
            .sumOf { it.daysFrozen }

        return 15 - frozenDaysThisYear
    }

    fun purchaseMembership(userId: String, membershipId: String): Boolean {
        val user = users.find { it.id == userId }
        val newMembership = memberships.find { it.id == membershipId }

        if (user != null && newMembership != null) {
            // Получаем текущий абонемент пользователя
            val currentMembership = user.membershipId?.let { membershipId ->
                memberships.find { it.id == membershipId }
            }

            // Проверяем, активен ли текущий абонемент
            val isCurrentActive = if (user.membershipExpiryDate != null) {
                val expiryDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(user.membershipExpiryDate)
                val today = Date()
                expiryDate != null && expiryDate.after(today)
            } else false

            // Если есть активный абонемент
            if (isCurrentActive && currentMembership != null) {
                // Сравниваем цены абонементов (чем выше цена, тем лучше абонемент)
                if (newMembership.price > currentMembership.price) {
                    // Апгрейд на более дорогой абонемент
                    Log.d("MEMBERSHIP", "Апгрейд абонемента: ${currentMembership.name} -> ${newMembership.name}")

                    // Рассчитываем остаток дней по текущему абонементу
                    val expiryDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(user.membershipExpiryDate)
                    val today = Date()
                    val daysLeft = if (expiryDate != null) {
                        val diff = expiryDate.time - today.time
                        max(0, TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).toInt())
                    } else 0

                    // Скидка за остаток дней (пропорционально)
                    val dailyPrice = currentMembership.price / currentMembership.durationDays.toDouble()
                    val discount = (dailyPrice * daysLeft).toInt()
                    val finalPrice = max(0, newMembership.price - discount)

                    Log.d("MEMBERSHIP", "Осталось дней: $daysLeft, скидка: $discount, итоговая цена: $finalPrice")

                    // Создаем платеж на разницу в цене
                    if (finalPrice > 0) {
                        val payment = Payment(
                            userId = userId,
                            amount = finalPrice,
                            paymentMethod = "upgrade",
                            description = "Апгрейд абонемента с ${currentMembership.name} на ${newMembership.name}"
                        )
                        payments.add(payment)
                    }

                    // Обновляем абонемент
                    val purchaseDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val newExpiryDate = getDateAfterDaysFromDate(purchaseDate, newMembership.durationDays)

                    val index = users.indexOf(user)
                    users[index] = user.copy(
                        membershipId = membershipId,
                        membershipPurchaseDate = purchaseDate,
                        membershipExpiryDate = newExpiryDate,
                        membershipFrozen = false,
                        membershipFrozenDate = null,
                        membershipFrozenDays = 0
                    )

                    saveAllData()
                    return true

                } else if (newMembership.price < currentMembership.price) {
                    // Попытка купить более дешевый абонемент
                    Log.d("MEMBERSHIP", "Попытка даунгрейда: ${currentMembership.name} -> ${newMembership.name}")
                    return false // Возвращаем false, чтобы показать сообщение о невозможности даунгрейда

                } else {
                    // Тот же абонемент - продление
                    Log.d("MEMBERSHIP", "Продление абонемента: ${currentMembership.name}")

                    val newExpiryDate = getDateAfterDaysFromDate(user.membershipExpiryDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                        newMembership.durationDays)

                    val index = users.indexOf(user)
                    users[index] = user.copy(
                        membershipExpiryDate = newExpiryDate
                    )

                    // Создаем платеж за продление
                    val payment = Payment(
                        userId = userId,
                        amount = newMembership.price,
                        paymentMethod = "extension",
                        description = "Продление абонемента ${newMembership.name}"
                    )
                    payments.add(payment)

                    saveAllData()
                    return true
                }
            } else {
                // Нет активного абонемента - обычная покупка
                Log.d("MEMBERSHIP", "Новая покупка абонемента: ${newMembership.name}")

                val purchaseDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val expiryDate = getDateAfterDaysFromDate(purchaseDate, newMembership.durationDays)

                val index = users.indexOf(user)
                users[index] = user.copy(
                    membershipId = membershipId,
                    membershipPurchaseDate = purchaseDate,
                    membershipExpiryDate = expiryDate,
                    membershipFrozen = false,
                    membershipFrozenDate = null,
                    membershipFrozenDays = 0
                )

                // Создаем платеж
                val payment = Payment(
                    userId = userId,
                    amount = newMembership.price,
                    paymentMethod = "purchase",
                    description = "Покупка абонемента ${newMembership.name}"
                )
                payments.add(payment)

                saveAllData()
                return true
            }
        }
        return false
    }

    fun getUserMembershipInfo(userId: String): Triple<Membership?, String?, String?> {
        val user = users.find { it.id == userId }
        if (user?.membershipId != null) {
            val membership = memberships.find { it.id == user.membershipId }
            return Triple(membership, user.membershipPurchaseDate, user.membershipExpiryDate)
        }
        return Triple(null, null, null)
    }

    fun extendMembership(userId: String): Boolean {
        val user = users.find { it.id == userId }
        if (user?.membershipId != null && user.membershipExpiryDate != null) {
            val membership = memberships.find { it.id == user.membershipId }
            if (membership != null) {
                val expiryDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(user.membershipExpiryDate)
                val today = Date()

                val newExpiryDate = if (expiryDate != null && expiryDate.after(today)) {
                    // Продлеваем от даты окончания
                    getDateAfterDaysFromDate(user.membershipExpiryDate, membership.durationDays)
                } else {
                    // Новый абонемент от сегодня
                    getDateAfterDaysFromDate(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()), membership.durationDays)
                }

                val index = users.indexOf(user)
                users[index] = user.copy(
                    membershipPurchaseDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    membershipExpiryDate = newExpiryDate
                )

                saveAllData()
                Log.d("MEMBERSHIP", "Абонемент продлен для пользователя $userId")
                return true
            }
        }
        return false
    }

    private fun getDateAfterDaysFromDate(startDate: String, days: Int): String {
        val calendar = Calendar.getInstance()
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(startDate)
        date?.let {
            calendar.time = it
            calendar.add(Calendar.DAY_OF_YEAR, days)
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }

    fun isValidDate(date: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.isLenient = false
            sdf.parse(date)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isValidTime(time: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.isLenient = false
            sdf.parse(time)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isFutureDate(date: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val inputDate = sdf.parse(date)
            val today = sdf.parse(sdf.format(Date()))
            inputDate != null && today != null && !inputDate.before(today)
        } catch (e: Exception) {
            false
        }
    }
}

// ==================== ГЛАВНАЯ АКТИВНОСТЬ ====================
class MainActivity : AppCompatActivity() {

    private lateinit var currentUser: User
    private var isLoggedIn = false
    private val requestCodePostNotifications = 1001

    // Для выбора фото
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            loadImageFromUri(it)
        }
    }

    private var tempProfilePhoto: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FitnessDatabase.init(this)

        NotificationHelper.createNotificationChannel(this)
        requestNotificationPermission()
        showLoginScreen()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    requestCodePostNotifications
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodePostNotifications) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Разрешение на уведомления получено", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Вы не будете получать уведомления", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadImageFromUri(uri: Uri) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true)
            tempProfilePhoto = PhotoHelper.bitmapToBase64(resizedBitmap)
            Toast.makeText(this, "Фото загружено", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка загрузки фото", Toast.LENGTH_SHORT).show()
        }
    }

    inner class PhoneNumberTextWatcher : TextWatcher {
        private var isFormatting = false

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            if (isFormatting || s == null) return
            isFormatting = true

            // 1. Оставляем только цифры
            var digits = s.toString().replace(Regex("[^\\d]"), "")

            // 2. Если пользователь ввел 7 или 8 в начале, убираем её,
            // так как +7 мы добавим сами в методе format
            if (digits.startsWith("7") || digits.startsWith("8")) {
                digits = if (digits.length > 1) digits.substring(1) else ""
            }

            // 3. Ограничиваем номер 10 цифрами
            val trimmed = if (digits.length > 10) digits.substring(0, 10) else digits

            // 4. Форматируем
            val formatted = formatPhoneNumber(trimmed)

            // 5. Обновляем текст и ставим курсор в конец
            if (formatted != s.toString()) {
                s.replace(0, s.length, formatted)
            }

            isFormatting = false
        }

        private fun formatPhoneNumber(digits: String): String {
            if (digits.isEmpty()) return ""

            val res = StringBuilder("+7 (")
            val len = digits.length

            // Добавляем код оператора (первые 3 цифры)
            res.append(digits.substring(0, minOf(len, 3)))

            // Если есть больше 3 цифр, закрываем скобку и добавляем блок из 3 цифр
            if (len > 3) {
                res.append(") ")
                res.append(digits.substring(3, minOf(len, 6)))
            }

            // Если есть больше 6 цифр, добавляем первый дефис и 2 цифры
            if (len > 6) {
                res.append("-")
                res.append(digits.substring(6, minOf(len, 8)))
            }

            // Если есть больше 8 цифр, добавляем второй дефис и последние цифры
            if (len > 8) {
                res.append("-")
                res.append(digits.substring(8, minOf(len, 10)))
            }

            return res.toString()
        }
    }

    private fun showLoginScreen() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)

            val title = TextView(this@MainActivity).apply {
                text = "🏋️‍♂️ Фитнес-Клуб"
                textSize = 28f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setTextColor(resources.getColor(android.R.color.holo_blue_dark))
                setPadding(0, 0, 0, 48)
            }
            addView(title)

            val etUsername = EditText(this@MainActivity).apply {
                hint = "Логин"
                setSingleLine(true)
                inputType = InputType.TYPE_CLASS_TEXT
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16 }
            }
            addView(etUsername)

            // Контейнер для поля пароля и кнопки глазика
            val passwordContainer = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 24 }

                // Фон для контейнера (как у EditText)
                setBackgroundResource(android.R.drawable.editbox_background)
                setPadding(8, 4, 8, 4)
            }

            val etPassword = EditText(this@MainActivity).apply {
                hint = "Пароль"
                // 👇 Пароль скрыт по умолчанию
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                setSingleLine(true)

                // 👇 Для API < 26 используем другой подход
                // Отключаем автозаполнение через inputType (частично работает)

                setBackgroundResource(android.R.color.transparent)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }
            passwordContainer.addView(etPassword)

            // Кнопка-глазик
            val btnTogglePassword = ImageButton(this@MainActivity).apply {
                setImageResource(android.R.drawable.ic_menu_view)
                setBackgroundResource(android.R.color.transparent)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                var isPasswordVisible = false

                setOnClickListener {
                    isPasswordVisible = !isPasswordVisible

                    if (isPasswordVisible) {
                        // Показываем пароль
                        etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                    } else {
                        // Скрываем пароль
                        etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        setImageResource(android.R.drawable.ic_menu_view)
                    }

                    // Сохраняем позицию курсора
                    etPassword.setSelection(etPassword.text.length)
                }
            }
            passwordContainer.addView(btnTogglePassword)

            addView(passwordContainer)

            val btnLogin = Button(this@MainActivity).apply {
                text = "Войти"
                setBackgroundColor(resources.getColor(android.R.color.holo_blue_dark))
                setTextColor(resources.getColor(android.R.color.white))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16 }
                setOnClickListener {
                    val username = etUsername.text.toString()
                    val password = etPassword.text.toString()

                    if (username.isNotEmpty() && password.isNotEmpty()) {
                        val user = FitnessDatabase.getUserByCredentials(username, password)
                        if (user != null) {
                            currentUser = user
                            isLoggedIn = true
                            showDashboard()
                        } else {
                            Toast.makeText(this@MainActivity, "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Заполните все поля", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            addView(btnLogin)

            val btnRegister = Button(this@MainActivity).apply {
                text = "Зарегистрироваться"
                setBackgroundColor(resources.getColor(android.R.color.darker_gray))
                setTextColor(resources.getColor(android.R.color.white))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener { showRegistrationDialog() }
            }
            addView(btnRegister)
        }
        setContentView(layout)
    }


    private fun showRegistrationDialog() {
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)

            // 0: Имя
            val etName = EditText(this@MainActivity).apply {
                hint = "Полное имя *"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }
            addView(etName)

            // 1: Логин
            val etUsername = EditText(this@MainActivity).apply {
                hint = "Логин *"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }
            addView(etUsername)

            // 2: Контейнер для пароля
            val passwordContainer = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 8 }
                setBackgroundResource(android.R.drawable.editbox_background)
                setPadding(8, 4, 8, 4)
            }

            val etPassword = EditText(this@MainActivity).apply {
                hint = "Пароль *"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                setBackgroundResource(android.R.color.transparent)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                // Убираем автофокус
                clearFocus()
            }
            passwordContainer.addView(etPassword)

            val btnTogglePassword = ImageButton(this@MainActivity).apply {
                setImageResource(android.R.drawable.ic_menu_view)
                setBackgroundResource(android.R.color.transparent)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                var isPasswordVisible = false

                setOnClickListener {
                    isPasswordVisible = !isPasswordVisible

                    if (isPasswordVisible) {
                        etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                    } else {
                        etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        setImageResource(android.R.drawable.ic_menu_view)
                    }
                    etPassword.setSelection(etPassword.text.length)
                }
            }
            passwordContainer.addView(btnTogglePassword)

            addView(passwordContainer)

            // 3: Контейнер для подтверждения пароля
            val confirmContainer = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
                setBackgroundResource(android.R.drawable.editbox_background)
                setPadding(8, 4, 8, 4)
            }

            val etConfirmPassword = EditText(this@MainActivity).apply {
                hint = "Подтвердите пароль *"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                setBackgroundResource(android.R.color.transparent)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                // Убираем автофокус
                clearFocus()
            }
            confirmContainer.addView(etConfirmPassword)

            val btnToggleConfirm = ImageButton(this@MainActivity).apply {
                setImageResource(android.R.drawable.ic_menu_view)
                setBackgroundResource(android.R.color.transparent)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                var isConfirmVisible = false

                setOnClickListener {
                    isConfirmVisible = !isConfirmVisible

                    if (isConfirmVisible) {
                        etConfirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                    } else {
                        etConfirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        setImageResource(android.R.drawable.ic_menu_view)
                    }
                    etConfirmPassword.setSelection(etConfirmPassword.text.length)
                }
            }
            confirmContainer.addView(btnToggleConfirm)

            addView(confirmContainer)

            // 4: Email
            val etEmail = EditText(this@MainActivity).apply {
                hint = "Email"
                inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }
            addView(etEmail)

            // 5: Телефон
            val etPhone = EditText(this@MainActivity).apply {
                hint = "+7 (999) 999-99-99"
                inputType = InputType.TYPE_CLASS_PHONE
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16 }

                // Добавляем фильтр для ограничения длины
                filters = arrayOf<InputFilter>(InputFilter.LengthFilter(18)) // максимальная длина с учетом форматирования

                addTextChangedListener(PhoneNumberTextWatcher())
            }
            addView(etPhone)

            // 6: Роль
            val roleSpinner = Spinner(this@MainActivity)
            val roles = arrayOf("Клиент", "Тренер")
            val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, roles)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            roleSpinner.adapter = adapter
            roleSpinner.setSelection(0)
            addView(roleSpinner)
        }

        AlertDialog.Builder(this)
            .setTitle("Регистрация")
            .setView(dialogLayout)
            .setPositiveButton("Зарегистрироваться") { dialog, which ->
                try {
                    // Получаем значения из полей по правильным индексам
                    val name = (dialogLayout.getChildAt(0) as EditText).text.toString()
                    val username = (dialogLayout.getChildAt(1) as EditText).text.toString()

                    // Получаем пароль из контейнера
                    val passwordContainer = dialogLayout.getChildAt(2) as LinearLayout
                    val etPassword = passwordContainer.getChildAt(0) as EditText
                    val password = etPassword.text.toString()

                    // Получаем подтверждение пароля из контейнера
                    val confirmContainer = dialogLayout.getChildAt(3) as LinearLayout
                    val etConfirmPassword = confirmContainer.getChildAt(0) as EditText
                    val confirmPassword = etConfirmPassword.text.toString()

                    val email = (dialogLayout.getChildAt(4) as EditText).text.toString()
                    val phone = (dialogLayout.getChildAt(5) as EditText).text.toString()
                    val role = (dialogLayout.getChildAt(6) as Spinner).selectedItem.toString()

                    // Проверка заполнения полей
                    if (name.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                        Toast.makeText(this@MainActivity, "Заполните обязательные поля", Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }

                    // Валидация пароля
                    if (password.length < 6) {
                        Toast.makeText(this@MainActivity, "Пароль должен быть не менее 6 символов (сейчас ${password.length})", Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }

                    if (!password.matches(Regex(".*[A-Za-z].*"))) {
                        Toast.makeText(this@MainActivity, "Пароль должен содержать хотя бы одну букву", Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }

                    if (!password.matches(Regex(".*\\d.*"))) {
                        Toast.makeText(this@MainActivity, "Пароль должен содержать хотя бы одну цифру", Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }

                    if (password != confirmPassword) {
                        Toast.makeText(this@MainActivity, "Пароли не совпадают!", Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }

                    if (!FitnessDatabase.isUsernameAvailable(username)) {
                        Toast.makeText(this@MainActivity, "Этот логин уже занят!", Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }

                    val newUser = User(
                        username = username,
                        passwordHash = PasswordHasher.hashPassword(password),
                        role = if (role == "Тренер") "trainer" else "client",
                        name = name,
                        email = email,
                        phone = phone
                    )

                    FitnessDatabase.users.add(newUser)

                    if (newUser.role == "trainer") {
                        FitnessDatabase.trainers.add(
                            Trainer(
                                userId = newUser.id,
                                name = newUser.name,
                                specialization = "Новый тренер",
                                experience = 0,
                                rating = 0.0
                            )
                        )
                    }

                    FitnessDatabase.saveAllData()

                    currentUser = newUser
                    isLoggedIn = true

                    Toast.makeText(this@MainActivity, "Регистрация успешна!", Toast.LENGTH_SHORT).show()
                    showDashboard()

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@MainActivity, "Ошибка при регистрации: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showDashboard() {
        when (currentUser.role) {
            "admin" -> showAdminDashboard()
            "trainer" -> showTrainerDashboard()
            "client" -> showClientDashboard()
        }
    }

    private fun showAdminDashboard() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)

            val title = TextView(this@MainActivity).apply {
                text = "👑 Панель администратора"
                textSize = 22f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 24)
            }
            addView(title)

            val welcome = TextView(this@MainActivity).apply {
                text = "Добро пожаловать, ${currentUser.name}!"
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 32)
            }
            addView(welcome)

            val btnAddTrainer = Button(this@MainActivity).apply {
                text = "➕ Добавить тренера"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
                setOnClickListener { showAddTrainerDialog() }
            }
            addView(btnAddTrainer)

            val btnEditTrainers = Button(this@MainActivity).apply {
                text = "✏️ Редактировать тренеров"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
                setOnClickListener { showEditTrainersList() }
            }
            addView(btnEditTrainers)

            val btnViewTrainers = Button(this@MainActivity).apply {
                text = "👥 Все тренеры"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
                setOnClickListener { showAllTrainers() }
            }
            addView(btnViewTrainers)

            val btnManageMemberships = Button(this@MainActivity).apply {
                text = "🎫 Управление абонементами"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
                setOnClickListener { showManageMemberships() }
            }
            addView(btnManageMemberships)

            val btnFreezeStats = Button(this@MainActivity).apply {
                text = "❄️ Статистика заморозок"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
                setOnClickListener { showFreezeStatistics() }
            }
            addView(btnFreezeStats)

            val btnReports = Button(this@MainActivity).apply {
                text = "📊 Отчеты и аналитика"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
                setOnClickListener { showReportsMenu() }
            }
            addView(btnReports)

            val btnStatistics = Button(this@MainActivity).apply {
                text = "📈 Статистика клуба"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
                setOnClickListener { showStatistics() }
            }
            addView(btnStatistics)

            val btnPayments = Button(this@MainActivity).apply {
                text = "💰 История платежей"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
                setOnClickListener { showAllPayments() }
            }
            addView(btnPayments)

            val btnLogout = Button(this@MainActivity).apply {
                text = "🚪 Выйти"
                setBackgroundColor(resources.getColor(android.R.color.holo_red_light))
                setTextColor(resources.getColor(android.R.color.white))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 32 }
                setOnClickListener {
                    isLoggedIn = false
                    showLoginScreen()
                }
            }
            addView(btnLogout)
        }

        setContentView(layout)
    }

    private fun showFreezeStatistics() {
        val frozenCount = FitnessDatabase.users.count { it.membershipFrozen }
        val totalFreezeHistory = FitnessDatabase.freezeHistory.size
        val totalFrozenDays = FitnessDatabase.freezeHistory.sumOf { it.daysFrozen }

        val message = """
            ❄️ СТАТИСТИКА ЗАМОРОЗОК
            
            • Сейчас заморожено абонементов: $frozenCount
            • Всего заморозок: $totalFreezeHistory
            • Всего дней заморозки: $totalFrozenDays
            
            📊 ПО ГОДАМ:
            ${
            FitnessDatabase.freezeHistory.groupBy { it.freezeDate.substring(0, 4) }
                .map { (year, list) ->
                    "   $year: ${list.size} заморозок, ${list.sumOf { it.daysFrozen }} дней"
                }.joinToString("\n")
        }
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("❄️ Статистика заморозок")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showEditTrainersList() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)

            val scrollView = ScrollView(this@MainActivity)
            val container = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
            }

            val title = TextView(this@MainActivity).apply {
                text = "✏️ Редактирование тренеров"
                textSize = 20f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 16)
            }
            container.addView(title)

            FitnessDatabase.trainers.forEach { trainer ->
                val card = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16, 16, 16, 16)
                    background = resources.getDrawable(android.R.drawable.dialog_holo_light_frame)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 12 }

                    val nameText = TextView(this@MainActivity).apply {
                        text = trainer.name
                        textSize = 18f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setPadding(0, 0, 0, 8)
                    }
                    addView(nameText)

                    val specText = TextView(this@MainActivity).apply {
                        text = "Специализация: ${trainer.specialization}"
                        setPadding(0, 0, 0, 4)
                    }
                    addView(specText)

                    val expText = TextView(this@MainActivity).apply {
                        text = "Опыт: ${trainer.experience} лет"
                        setPadding(0, 0, 0, 4)
                    }
                    addView(expText)

                    val ratingText = TextView(this@MainActivity).apply {
                        text = "Рейтинг: ${trainer.rating}/5.0"
                        setPadding(0, 0, 0, 8)
                    }
                    addView(ratingText)

                    val btnEdit = Button(this@MainActivity).apply {
                        text = "✏️ Редактировать"
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        setOnClickListener { showEditTrainerDialog(trainer) }
                    }
                    addView(btnEdit)
                }
                container.addView(card)
            }

            val btnBack = Button(this@MainActivity).apply {
                text = "Назад"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 16 }
                setOnClickListener { showAdminDashboard() }
            }
            container.addView(btnBack)

            scrollView.addView(container)
            addView(scrollView)
        }

        setContentView(layout)
    }

    private fun showEditTrainerDialog(trainer: Trainer) {
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)

            val etName = EditText(this@MainActivity).apply {
                setText(trainer.name)
                hint = "Имя тренера"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }
            addView(etName)

            val etSpecialization = EditText(this@MainActivity).apply {
                setText(trainer.specialization)
                hint = "Специализация"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }
            addView(etSpecialization)

            val etExperience = EditText(this@MainActivity).apply {
                setText(trainer.experience.toString())
                hint = "Опыт работы (лет)"
                inputType = InputType.TYPE_CLASS_NUMBER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }
            addView(etExperience)

            val etDescription = EditText(this@MainActivity).apply {
                setText(trainer.description)
                hint = "Описание"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }
            addView(etDescription)
        }

        AlertDialog.Builder(this)
            .setTitle("Редактировать тренера")
            .setView(dialogLayout)
            .setPositiveButton("Сохранить") { dialog, which ->
                val name = (dialogLayout.getChildAt(0) as EditText).text.toString()
                val specialization = (dialogLayout.getChildAt(1) as EditText).text.toString()
                val experience = (dialogLayout.getChildAt(2) as EditText).text.toString().toIntOrNull() ?: trainer.experience
                val description = (dialogLayout.getChildAt(3) as EditText).text.toString()

                if (name.isNotEmpty()) {
                    val success = FitnessDatabase.updateTrainer(
                        trainer.id,
                        name,
                        specialization,
                        experience,
                        description
                    )

                    if (success) {
                        Toast.makeText(this@MainActivity, "Данные тренера обновлены!", Toast.LENGTH_SHORT).show()
                        showEditTrainersList()
                    } else {
                        Toast.makeText(this@MainActivity, "Ошибка при обновлении", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Имя не может быть пустым", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showManageMemberships() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)

            val scrollView = ScrollView(this@MainActivity)
            val container = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
            }

            val title = TextView(this@MainActivity).apply {
                text = "🎫 Управление абонементами"
                textSize = 20f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 16)
            }
            container.addView(title)

            val btnAddMembership = Button(this@MainActivity).apply {
                text = "➕ Добавить абонемент"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16 }
                setOnClickListener { showAddMembershipDialog() }
            }
            container.addView(btnAddMembership)

            FitnessDatabase.memberships.forEach { membership ->
                val card = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16, 16, 16, 16)
                    background = resources.getDrawable(android.R.drawable.dialog_holo_light_frame)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 12 }

                    val nameText = TextView(this@MainActivity).apply {
                        text = membership.name
                        textSize = 18f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                    }
                    addView(nameText)

                    val detailsText = TextView(this@MainActivity).apply {
                        text = "💰 ${membership.price} руб. | 📅 ${membership.durationDays} дн. | 🏋️ ${membership.workoutsCount} тренировок"
                        setPadding(0, 8, 0, 8)
                    }
                    addView(detailsText)

                    val descText = TextView(this@MainActivity).apply {
                        text = membership.description
                        setTextColor(resources.getColor(android.R.color.darker_gray))
                        setPadding(0, 0, 0, 8)
                    }
                    addView(descText)

                    val btnEdit = Button(this@MainActivity).apply {
                        text = "✏️ Редактировать"
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = 8 }
                        setOnClickListener { showEditMembershipDialog(membership) }
                    }
                    addView(btnEdit)

                    val btnDelete = Button(this@MainActivity).apply {
                        text = "🗑️ Удалить"
                        setBackgroundColor(resources.getColor(android.R.color.holo_red_light))
                        setTextColor(resources.getColor(android.R.color.white))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        setOnClickListener {
                            FitnessDatabase.memberships.remove(membership)
                            FitnessDatabase.saveAllData()
                            Toast.makeText(this@MainActivity, "Абонемент удален", Toast.LENGTH_SHORT).show()
                            showManageMemberships()
                        }
                    }
                    addView(btnDelete)
                }
                container.addView(card)
            }

            val btnBack = Button(this@MainActivity).apply {
                text = "Назад"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 16 }
                setOnClickListener { showAdminDashboard() }
            }
            container.addView(btnBack)

            scrollView.addView(container)
            addView(scrollView)
        }

        setContentView(layout)
    }

    private fun showAddMembershipDialog() {
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)

            val etName = EditText(this@MainActivity).apply {
                hint = "Название абонемента"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }
            addView(etName)

            val etDuration = EditText(this@MainActivity).apply {
                hint = "Длительность (дней)"
                inputType = InputType.TYPE_CLASS_NUMBER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }
            addView(etDuration)

            val etPrice = EditText(this@MainActivity).apply {
                hint = "Цена (руб.)"
                inputType = InputType.TYPE_CLASS_NUMBER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }
            addView(etPrice)

            val etWorkoutsCount = EditText(this@MainActivity).apply {
                hint = "Количество тренировок"
                inputType = InputType.TYPE_CLASS_NUMBER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }
            addView(etWorkoutsCount)

            val etDescription = EditText(this@MainActivity).apply {
                hint = "Описание"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            addView(etDescription)
        }

        AlertDialog.Builder(this)
            .setTitle("Добавить абонемент")
            .setView(dialogLayout)
            .setPositiveButton("Добавить") { dialog, which ->
                val name = (dialogLayout.getChildAt(0) as EditText).text.toString()
                val duration = (dialogLayout.getChildAt(1) as EditText).text.toString().toIntOrNull() ?: 30
                val price = (dialogLayout.getChildAt(2) as EditText).text.toString().toIntOrNull() ?: 0
                val workoutsCount = (dialogLayout.getChildAt(3) as EditText).text.toString().toIntOrNull() ?: 0
                val description = (dialogLayout.getChildAt(4) as EditText).text.toString()

                if (name.isNotEmpty()) {
                    val membership = Membership(
                        name = name,
                        durationDays = duration,
                        price = price,
                        workoutsCount = workoutsCount,
                        description = description
                    )

                    FitnessDatabase.memberships.add(membership)
                    FitnessDatabase.saveAllData()
                    Toast.makeText(this@MainActivity, "Абонемент добавлен!", Toast.LENGTH_SHORT).show()
                    showManageMemberships()
                } else {
                    Toast.makeText(this@MainActivity, "Введите название", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditMembershipDialog(membership: Membership) {
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)

            val etName = EditText(this@MainActivity).apply {
                setText(membership.name)
                hint = "Название абонемента"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }
            addView(etName)

            val etDuration = EditText(this@MainActivity).apply {
                setText(membership.durationDays.toString())
                hint = "Длительность (дней)"
                inputType = InputType.TYPE_CLASS_NUMBER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }
            addView(etDuration)

            val etPrice = EditText(this@MainActivity).apply {
                setText(membership.price.toString())
                hint = "Цена (руб.)"
                inputType = InputType.TYPE_CLASS_NUMBER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }
            addView(etPrice)

            val etWorkoutsCount = EditText(this@MainActivity).apply {
                setText(membership.workoutsCount.toString())
                hint = "Количество тренировок"
                inputType = InputType.TYPE_CLASS_NUMBER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }
            addView(etWorkoutsCount)

            val etDescription = EditText(this@MainActivity).apply {
                setText(membership.description)
                hint = "Описание"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            addView(etDescription)
        }

        AlertDialog.Builder(this)
            .setTitle("Редактировать абонемент")
            .setView(dialogLayout)
            .setPositiveButton("Сохранить") { dialog, which ->
                val name = (dialogLayout.getChildAt(0) as EditText).text.toString()
                val duration = (dialogLayout.getChildAt(1) as EditText).text.toString().toIntOrNull() ?: membership.durationDays
                val price = (dialogLayout.getChildAt(2) as EditText).text.toString().toIntOrNull() ?: membership.price
                val workoutsCount = (dialogLayout.getChildAt(3) as EditText).text.toString().toIntOrNull() ?: membership.workoutsCount
                val description = (dialogLayout.getChildAt(4) as EditText).text.toString()

                if (name.isNotEmpty()) {
                    val index = FitnessDatabase.memberships.indexOf(membership)
                    if (index != -1) {
                        FitnessDatabase.memberships[index] = membership.copy(
                            name = name,
                            durationDays = duration,
                            price = price,
                            workoutsCount = workoutsCount,
                            description = description
                        )
                        FitnessDatabase.saveAllData()
                        Toast.makeText(this@MainActivity, "Абонемент обновлен!", Toast.LENGTH_SHORT).show()
                        showManageMemberships()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Введите название", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showReportsMenu() {
        val options = arrayOf(
            "📊 Отчет за неделю",
            "📊 Отчет за месяц",
            "📊 Отчет за год",
            "📊 Выбрать период"
        )

        AlertDialog.Builder(this)
            .setTitle("Выберите период отчета")
            .setItems(options) { dialog, which ->
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val calendar = Calendar.getInstance()

                val (startDate, endDate) = when (which) {
                    0 -> {
                        calendar.add(Calendar.DAY_OF_YEAR, -7)
                        val start = calendar.get(Calendar.YEAR).toString() + "-" +
                                String.format("%02d", calendar.get(Calendar.MONTH) + 1) + "-" +
                                String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))
                        Pair(start, today)
                    }
                    1 -> {
                        calendar.add(Calendar.MONTH, -1)
                        val start = calendar.get(Calendar.YEAR).toString() + "-" +
                                String.format("%02d", calendar.get(Calendar.MONTH) + 1) + "-" +
                                String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))
                        Pair(start, today)
                    }
                    2 -> {
                        calendar.add(Calendar.YEAR, -1)
                        val start = calendar.get(Calendar.YEAR).toString() + "-" +
                                String.format("%02d", calendar.get(Calendar.MONTH) + 1) + "-" +
                                String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))
                        Pair(start, today)
                    }
                    else -> {
                        showCustomPeriodDialog()
                        return@setItems
                    }
                }

                val report = ReportGenerator.generateReport(startDate, endDate)
                showReportDialog(report)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showCustomPeriodDialog() {
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)

            val tvStartHint = TextView(this@MainActivity).apply {
                text = "Начало периода (ГГГГ-ММ-ДД):"
                setPadding(0, 0, 0, 8)
            }
            addView(tvStartHint)

            val etStartDate = EditText(this@MainActivity).apply {
                hint = "Например: 2024-01-01"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16 }
            }
            addView(etStartDate)

            val tvEndHint = TextView(this@MainActivity).apply {
                text = "Конец периода (ГГГГ-ММ-ДД):"
                setPadding(0, 0, 0, 8)
            }
            addView(tvEndHint)

            val etEndDate = EditText(this@MainActivity).apply {
                hint = "Например: 2024-12-31"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            addView(etEndDate)
        }

        AlertDialog.Builder(this)
            .setTitle("Выберите период")
            .setView(dialogLayout)
            .setPositiveButton("Сформировать") { dialog, which ->
                val startDate = (dialogLayout.getChildAt(1) as EditText).text.toString()
                val endDate = (dialogLayout.getChildAt(3) as EditText).text.toString()

                if (startDate.isNotEmpty() && endDate.isNotEmpty()) {
                    val report = ReportGenerator.generateReport(startDate, endDate)
                    showReportDialog(report)
                } else {
                    Toast.makeText(this@MainActivity, "Заполните даты", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showReportDialog(report: ReportGenerator.ClubReport) {
        val popularTrainersText = if (report.popularTrainers.isEmpty()) {
            "Нет данных"
        } else {
            report.popularTrainers.joinToString("\n") { "• ${it.first}: ${it.second} записей" }
        }

        val popularWorkoutsText = if (report.popularWorkouts.isEmpty()) {
            "Нет данных"
        } else {
            report.popularWorkouts.joinToString("\n") { "• ${it.first}: ${it.second} записей" }
        }

        val popularReviewsText = if (report.popularWorkoutReviews.isEmpty()) {
            "Нет данных"
        } else {
            report.popularWorkoutReviews.joinToString("\n") { "• ${it.first}: ${String.format("%.1f", it.second)} ⭐" }
        }

        val message = """
            📋 ОТЧЕТ ЗА ПЕРИОД
            С: ${report.periodStart}
            По: ${report.periodEnd}
            
            👥 ОБЩАЯ СТАТИСТИКА:
            • Всего пользователей: ${report.totalUsers}
            • Тренеров: ${report.totalTrainers}
            • Клиентов: ${report.totalClients}
            
            📅 ТРЕНИРОВКИ:
            • Проведено тренировок: ${report.totalWorkouts}
            • Всего записей: ${report.totalBookings}
            
            💰 ФИНАНСЫ:
            • Общий доход: ${report.totalRevenue} руб.
            
            ❄️ ЗАМОРОЗКИ:
            • Заморожено абонементов: ${report.totalFrozenMemberships}
            
            🔥 ПОПУЛЯРНЫЕ ТРЕНЕРЫ:
            ${popularTrainersText}
            
            🏋️ ПОПУЛЯРНЫЕ ТРЕНИРОВКИ:
            ${popularWorkoutsText}
            
            ⭐ ЛУЧШИЕ ТРЕНИРОВКИ ПО ОТЗЫВАМ:
            ${popularReviewsText}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("📊 Отчет клуба")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAllPayments() {
        val payments = PaymentProcessor.getAllPayments()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)

            val scrollView = ScrollView(this@MainActivity)
            val container = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
            }

            val title = TextView(this@MainActivity).apply {
                text = "💰 История платежей"
                textSize = 20f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 16)
            }
            container.addView(title)

            val totalRevenue = payments.filter { it.status == "completed" }.sumOf { it.amount }

            val totalText = TextView(this@MainActivity).apply {
                text = "Общий доход: $totalRevenue руб."
                textSize = 18f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setTextColor(resources.getColor(android.R.color.holo_green_dark))
                setPadding(0, 0, 0, 16)
            }
            container.addView(totalText)

            if (payments.isEmpty()) {
                val emptyText = TextView(this@MainActivity).apply {
                    text = "Нет платежей"
                    gravity = Gravity.CENTER
                    setPadding(0, 32, 0, 32)
                }
                container.addView(emptyText)
            } else {
                payments.reversed().forEach { payment ->
                    val user = FitnessDatabase.users.find { it.id == payment.userId }

                    val card = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(16, 16, 16, 16)
                        background = resources.getDrawable(android.R.drawable.dialog_holo_light_frame)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = 12 }

                        val userText = TextView(this@MainActivity).apply {
                            text = "👤 ${user?.name ?: "Неизвестно"}"
                            textSize = 16f
                            setTypeface(typeface, android.graphics.Typeface.BOLD)
                        }
                        addView(userText)

                        val amountText = TextView(this@MainActivity).apply {
                            text = "💰 ${payment.amount} руб."
                            textSize = 18f
                            setTypeface(typeface, android.graphics.Typeface.BOLD)
                            setTextColor(resources.getColor(android.R.color.holo_green_dark))
                        }
                        addView(amountText)

                        val descText = TextView(this@MainActivity).apply {
                            text = "📝 ${payment.description}"
                            setPadding(0, 4, 0, 4)
                        }
                        addView(descText)

                        val methodText = TextView(this@MainActivity).apply {
                            text = "💳 ${payment.paymentMethod}"
                            setPadding(0, 4, 0, 4)
                        }
                        addView(methodText)

                        val dateText = TextView(this@MainActivity).apply {
                            text = "📅 ${payment.paymentDate}"
                            textSize = 12f
                            setTextColor(resources.getColor(android.R.color.darker_gray))
                        }
                        addView(dateText)
                    }
                    container.addView(card)
                }
            }

            val btnBack = Button(this@MainActivity).apply {
                text = "Назад"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 16 }
                setOnClickListener { showAdminDashboard() }
            }
            container.addView(btnBack)

            scrollView.addView(container)
            addView(scrollView)
        }

        setContentView(layout)
    }

    private fun showAddTrainerDialog() {
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)

            val etName = EditText(this@MainActivity).apply {
                hint = "Имя тренера"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }
            addView(etName)

            val etSpecialization = EditText(this@MainActivity).apply {
                hint = "Специализация"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }
            addView(etSpecialization)

            val etExperience = EditText(this@MainActivity).apply {
                hint = "Опыт работы (лет)"
                inputType = InputType.TYPE_CLASS_NUMBER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }
            addView(etExperience)

            val etDescription = EditText(this@MainActivity).apply {
                hint = "Описание"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }
            addView(etDescription)

            val etEmail = EditText(this@MainActivity).apply {
                hint = "Email"
                inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }
            addView(etEmail)

            val etPhone = EditText(this@MainActivity).apply {
                hint = "Телефон"
                inputType = InputType.TYPE_CLASS_PHONE
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            addView(etPhone)
        }

        AlertDialog.Builder(this)
            .setTitle("Добавить нового тренера")
            .setView(dialogLayout)
            .setPositiveButton("Добавить") { dialog, which ->
                val name = (dialogLayout.getChildAt(0) as EditText).text.toString()
                val specialization = (dialogLayout.getChildAt(1) as EditText).text.toString()
                val experience = (dialogLayout.getChildAt(2) as EditText).text.toString().toIntOrNull() ?: 0
                val description = (dialogLayout.getChildAt(3) as EditText).text.toString()
                val email = (dialogLayout.getChildAt(4) as EditText).text.toString()
                val phone = (dialogLayout.getChildAt(5) as EditText).text.toString()

                if (name.isNotEmpty()) {
                    val username = name.lowercase(Locale.getDefault()).replace(" ", "") + "_trainer"
                    val password = "temp123"

                    val trainerUser = User(
                        username = username,
                        passwordHash = PasswordHasher.hashPassword(password),
                        role = "trainer",
                        name = name,
                        email = email,
                        phone = phone
                    )

                    FitnessDatabase.users.add(trainerUser)

                    val trainer = Trainer(
                        userId = trainerUser.id,
                        name = name,
                        specialization = if (specialization.isNotEmpty()) specialization else "Фитнес",
                        experience = experience,
                        description = description
                    )

                    FitnessDatabase.trainers.add(trainer)
                    FitnessDatabase.saveAllData()

                    Toast.makeText(
                        this@MainActivity,
                        "Тренер добавлен!\nЛогин: $username\nПароль: $password",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showAllTrainers() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)

            val scrollView = ScrollView(this@MainActivity)
            val container = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
            }

            val title = TextView(this@MainActivity).apply {
                text = "👥 Все тренеры"
                textSize = 20f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 16)
            }
            container.addView(title)

            FitnessDatabase.trainers.forEach { trainer ->
                val reviews = FitnessDatabase.getReviewsForTrainer(trainer.id)
                val avgRating = FitnessDatabase.getAverageRating(trainer.id)

                val card = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16, 16, 16, 16)
                    background = resources.getDrawable(android.R.drawable.dialog_holo_light_frame)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 12 }

                    // Фото тренера (если есть)
                    if (trainer.photoUrl != null) {
                        val bitmap = PhotoHelper.base64ToBitmap(trainer.photoUrl)
                        val imageView = ImageView(this@MainActivity).apply {
                            setImageBitmap(bitmap)
                            layoutParams = LinearLayout.LayoutParams(
                                200, 200
                            ).apply {
                                gravity = Gravity.CENTER
                                bottomMargin = 8
                            }
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                        addView(imageView)
                    }

                    val name = TextView(this@MainActivity).apply {
                        text = trainer.name
                        textSize = 18f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                    }
                    addView(name)

                    val specialization = TextView(this@MainActivity).apply {
                        text = "🏆 ${trainer.specialization}"
                        setPadding(0, 4, 0, 4)
                    }
                    addView(specialization)

                    val experience = TextView(this@MainActivity).apply {
                        text = "⏱️ Опыт: ${trainer.experience} лет"
                        setPadding(0, 4, 0, 4)
                    }
                    addView(experience)

                    val rating = TextView(this@MainActivity).apply {
                        text = "⭐ Рейтинг: ${String.format("%.1f", avgRating)}/5.0 (${reviews.size} отзывов)"
                        setPadding(0, 4, 0, 4)
                        setTextColor(resources.getColor(android.R.color.holo_orange_dark))
                    }
                    addView(rating)

                    setOnClickListener { showTrainerDetails(trainer, true) }
                }
                container.addView(card)
            }

            val btnBack = Button(this@MainActivity).apply {
                text = "Назад"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 16 }
                setOnClickListener { showAdminDashboard() }
            }
            container.addView(btnBack)

            scrollView.addView(container)
            addView(scrollView)
        }

        setContentView(layout)
    }

    private fun showStatistics() {
        val totalTrainers = FitnessDatabase.trainers.size
        val totalWorkouts = FitnessDatabase.workouts.count { it.isActive }
        val totalClients = FitnessDatabase.users.count { it.role == "client" }
        val totalBookings = FitnessDatabase.bookings.count { it.status == "confirmed" }
        val totalRevenue = FitnessDatabase.payments.filter { it.status == "completed" }.sumOf { it.amount }
        val totalFrozen = FitnessDatabase.users.count { it.membershipFrozen }

        val availableWorkouts = FitnessDatabase.workouts.count { it.isAvailable && it.isActive }
        val avgParticipants = if (FitnessDatabase.workouts.isNotEmpty()) {
            FitnessDatabase.workouts.filter { it.isActive }.map { it.currentParticipants.toDouble() / it.maxParticipants }.average() * 100
        } else 0.0

        val message = """
            📊 СТАТИСТИКА ФИТНЕС-КЛУБА
            
            👥 ПЕРСОНАЛ:
            • Тренеров: $totalTrainers
            • Клиентов: $totalClients
            
            📅 ТРЕНИРОВКИ:
            • Всего тренировок: $totalWorkouts
            • Доступных тренировок: $availableWorkouts
            • Активных бронирований: $totalBookings
            
            📈 ЭФФЕКТИВНОСТЬ:
            • Средняя заполняемость: ${String.format("%.1f", avgParticipants)}%
            
            💰 ФИНАНСЫ:
            • Общий доход: $totalRevenue руб.
            
            ❄️ ЗАМОРОЗКИ:
            • Заморожено абонементов: $totalFrozen
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("📊 Статистика клуба")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showTrainerDashboard() {
        val trainer = FitnessDatabase.getTrainerByUserId(currentUser.id)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)

            val title = TextView(this@MainActivity).apply {
                text = "🏋️ Панель тренера"
                textSize = 22f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 24)
            }
            addView(title)

            val welcome = TextView(this@MainActivity).apply {
                text = "Добро пожаловать, ${trainer?.name ?: currentUser.name}!"
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 32)
            }
            addView(welcome)

            val btnCreateWorkout = Button(this@MainActivity).apply {
                text = "➕ Создать тренировку"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
                setOnClickListener { showCreateWorkoutDialog() }
            }
            addView(btnCreateWorkout)

            val btnMyWorkouts = Button(this@MainActivity).apply {
                text = "📅 Мои тренировки"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
                setOnClickListener { showMyWorkouts() }
            }
            addView(btnMyWorkouts)

            val btnMySchedule = Button(this@MainActivity).apply {
                text = "🗓️ Мое расписание"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
                setOnClickListener { showTrainerSchedule() }
            }
            addView(btnMySchedule)

            val btnMyClients = Button(this@MainActivity).apply {
                text = "👥 Мои клиенты"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
                setOnClickListener { showMyClients() }
            }
            addView(btnMyClients)

            val btnEditProfile = Button(this@MainActivity).apply {
                text = "✏️ Редактировать профиль"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
                setOnClickListener { showEditProfileDialog() }
            }
            addView(btnEditProfile)

            val btnProfile = Button(this@MainActivity).apply {
                text = "👤 Мой профиль"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
                setOnClickListener { showTrainerProfile() }
            }
            addView(btnProfile)

            val btnLogout = Button(this@MainActivity).apply {
                text = "🚪 Выйти"
                setBackgroundColor(resources.getColor(android.R.color.holo_red_light))
                setTextColor(resources.getColor(android.R.color.white))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 32 }
                setOnClickListener {
                    isLoggedIn = false
                    showLoginScreen()
                }
            }
            addView(btnLogout)
        }

        setContentView(layout)
    }

    private fun showCreateWorkoutDialog() {
        val trainer = FitnessDatabase.getTrainerByUserId(currentUser.id) ?: return

        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        // Название тренировки
        val etTitle = EditText(this@MainActivity).apply {
            hint = "Название тренировки"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }
        dialogLayout.addView(etTitle)

        // Описание тренировки
        val etDescription = EditText(this@MainActivity).apply {
            hint = "Описание тренировки"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }
        dialogLayout.addView(etDescription)

        // Поле для даты с возможностью выбора из календаря
        val tvDateHint = TextView(this@MainActivity).apply {
            text = "Дата:"
            textSize = 14f
            setTextColor(resources.getColor(android.R.color.darker_gray))
            setPadding(0, 8, 0, 4)
        }
        dialogLayout.addView(tvDateHint)

        val etDate = EditText(this@MainActivity).apply {
            hint = "Выберите дату"
            isFocusable = false
            isClickable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }
        dialogLayout.addView(etDate)

        // Устанавливаем слушатель отдельно для даты
        etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this@MainActivity)
            datePicker.setOnDateSetListener { view, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                etDate.setText(selectedDate)
            }
            datePicker.datePicker.updateDate(year, month, day)
            datePicker.show()
        }

        // Поле для времени с возможностью выбора из часов
        val tvTimeHint = TextView(this@MainActivity).apply {
            text = "Время:"
            textSize = 14f
            setTextColor(resources.getColor(android.R.color.darker_gray))
            setPadding(0, 8, 0, 4)
        }
        dialogLayout.addView(tvTimeHint)

        val etTime = EditText(this@MainActivity).apply {
            hint = "Выберите время"
            isFocusable = false
            isClickable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }
        dialogLayout.addView(etTime)

        // Устанавливаем слушатель отдельно для времени
        etTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePicker = TimePickerDialog(this@MainActivity,
                { view, selectedHour, selectedMinute ->
                    val selectedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                    etTime.setText(selectedTime)
                }, hour, minute, true)
            timePicker.show()
        }

        // Длительность
        val etDuration = EditText(this@MainActivity).apply {
            hint = "Длительность (минуты)"
            inputType = InputType.TYPE_CLASS_NUMBER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }
        dialogLayout.addView(etDuration)

        // Максимум участников
        val etMaxParticipants = EditText(this@MainActivity).apply {
            hint = "Максимум участников"
            inputType = InputType.TYPE_CLASS_NUMBER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }
        dialogLayout.addView(etMaxParticipants)

        // Место проведения
        val etLocation = EditText(this@MainActivity).apply {
            hint = "Место проведения"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }
        dialogLayout.addView(etLocation)

        // Цена
        val etPrice = EditText(this@MainActivity).apply {
            hint = "Цена (руб.)"
            inputType = InputType.TYPE_CLASS_NUMBER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        dialogLayout.addView(etPrice)

        AlertDialog.Builder(this)
            .setTitle("Создать новую тренировку")
            .setView(dialogLayout)
            .setPositiveButton("Создать") { dialog, which ->
                val title = etTitle.text.toString()
                val description = etDescription.text.toString()
                val date = etDate.text.toString()
                val time = etTime.text.toString()
                val duration = etDuration.text.toString().toIntOrNull() ?: 60
                val maxParticipants = etMaxParticipants.text.toString().toIntOrNull() ?: 10
                val location = etLocation.text.toString()
                val price = etPrice.text.toString().toIntOrNull() ?: 500

                // Валидация даты и времени
                if (date.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Выберите дату", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                if (time.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Выберите время", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                if (!FitnessDatabase.isValidDate(date)) {
                    Toast.makeText(this@MainActivity, "Неверный формат даты! Используйте ГГГГ-ММ-ДД", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                if (!FitnessDatabase.isValidTime(time)) {
                    Toast.makeText(this@MainActivity, "Неверный формат времени! Используйте ЧЧ:ММ", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                if (!FitnessDatabase.isFutureDate(date)) {
                    Toast.makeText(this@MainActivity, "Дата не может быть в прошлом!", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                if (title.isNotEmpty()) {
                    val workout = Workout(
                        trainerId = trainer.id,
                        title = title,
                        description = description,
                        date = date,
                        time = time,
                        duration = duration,
                        maxParticipants = maxParticipants,
                        location = if (location.isNotEmpty()) location else "Зал 1",
                        price = price
                    )

                    FitnessDatabase.workouts.add(workout)
                    FitnessDatabase.saveAllData()

                    try {
                        val workoutDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            .parse("$date $time")
                        workoutDateTime?.let {
                            val reminderTime = it.time - 3600000
                            NotificationHelper.scheduleWorkoutReminder(this@MainActivity, workout, reminderTime)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    Toast.makeText(this@MainActivity, "Тренировка создана! Установлено напоминание.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditWorkoutDialog(workout: Workout) {
        val trainer = FitnessDatabase.getTrainerByUserId(currentUser.id) ?: return

        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        // Название тренировки
        val etTitle = EditText(this@MainActivity).apply {
            setText(workout.title)
            hint = "Название тренировки"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }
        dialogLayout.addView(etTitle)

        // Описание тренировки
        val etDescription = EditText(this@MainActivity).apply {
            setText(workout.description)
            hint = "Описание тренировки"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }
        dialogLayout.addView(etDescription)

        // Поле для даты с возможностью выбора из календаря
        val tvDateHint = TextView(this@MainActivity).apply {
            text = "Дата:"
            textSize = 14f
            setTextColor(resources.getColor(android.R.color.darker_gray))
            setPadding(0, 8, 0, 4)
        }
        dialogLayout.addView(tvDateHint)

        val etDate = EditText(this@MainActivity).apply {
            setText(workout.date)
            hint = "Выберите дату"
            isFocusable = false
            isClickable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }
        dialogLayout.addView(etDate)

        // Устанавливаем слушатель отдельно для даты
        etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val currentDate = workout.date.split("-").map { it.toInt() }
            val year = if (currentDate.size == 3) currentDate[0] else calendar.get(Calendar.YEAR)
            val month = if (currentDate.size == 3) currentDate[1] - 1 else calendar.get(Calendar.MONTH)
            val day = if (currentDate.size == 3) currentDate[2] else calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this@MainActivity)
            datePicker.setOnDateSetListener { view, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                etDate.setText(selectedDate)
            }
            datePicker.datePicker.updateDate(year, month, day)
            datePicker.show()
        }

        // Поле для времени с возможностью выбора из часов
        val tvTimeHint = TextView(this@MainActivity).apply {
            text = "Время:"
            textSize = 14f
            setTextColor(resources.getColor(android.R.color.darker_gray))
            setPadding(0, 8, 0, 4)
        }
        dialogLayout.addView(tvTimeHint)

        val etTime = EditText(this@MainActivity).apply {
            setText(workout.time)
            hint = "Выберите время"
            isFocusable = false
            isClickable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }
        dialogLayout.addView(etTime)

        // Устанавливаем слушатель отдельно для времени
        etTime.setOnClickListener {
            val currentTime = workout.time.split(":").map { it.toInt() }
            val hour = if (currentTime.size == 2) currentTime[0] else 12
            val minute = if (currentTime.size == 2) currentTime[1] else 0

            val timePicker = TimePickerDialog(this@MainActivity,
                { view, selectedHour, selectedMinute ->
                    val selectedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                    etTime.setText(selectedTime)
                }, hour, minute, true)
            timePicker.show()
        }

        // Длительность
        val etDuration = EditText(this@MainActivity).apply {
            setText(workout.duration.toString())
            hint = "Длительность (минуты)"
            inputType = InputType.TYPE_CLASS_NUMBER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }
        dialogLayout.addView(etDuration)

        // Максимум участников
        val etMaxParticipants = EditText(this@MainActivity).apply {
            setText(workout.maxParticipants.toString())
            hint = "Максимум участников"
            inputType = InputType.TYPE_CLASS_NUMBER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }
        dialogLayout.addView(etMaxParticipants)

        // Место проведения
        val etLocation = EditText(this@MainActivity).apply {
            setText(workout.location)
            hint = "Место проведения"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }
        dialogLayout.addView(etLocation)

        // Цена
        val etPrice = EditText(this@MainActivity).apply {
            setText(workout.price.toString())
            hint = "Цена (руб.)"
            inputType = InputType.TYPE_CLASS_NUMBER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        dialogLayout.addView(etPrice)

        AlertDialog.Builder(this)
            .setTitle("Редактировать тренировку")
            .setView(dialogLayout)
            .setPositiveButton("Сохранить") { dialog, which ->
                val title = etTitle.text.toString()
                val description = etDescription.text.toString()
                val date = etDate.text.toString()
                val time = etTime.text.toString()
                val duration = etDuration.text.toString().toIntOrNull() ?: workout.duration
                val maxParticipants = etMaxParticipants.text.toString().toIntOrNull() ?: workout.maxParticipants
                val location = etLocation.text.toString()
                val price = etPrice.text.toString().toIntOrNull() ?: workout.price

                // Валидация даты и времени
                if (date.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Выберите дату", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                if (time.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Выберите время", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                if (!FitnessDatabase.isValidDate(date)) {
                    Toast.makeText(this@MainActivity, "Неверный формат даты! Используйте ГГГГ-ММ-ДД", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                if (!FitnessDatabase.isValidTime(time)) {
                    Toast.makeText(this@MainActivity, "Неверный формат времени! Используйте ЧЧ:ММ", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                if (!FitnessDatabase.isFutureDate(date)) {
                    Toast.makeText(this@MainActivity, "Дата не может быть в прошлом!", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                if (title.isNotEmpty()) {
                    val success = FitnessDatabase.updateWorkout(
                        workout.id,
                        title,
                        description,
                        date,
                        time,
                        duration,
                        maxParticipants,
                        location,
                        price
                    )

                    if (success) {
                        Toast.makeText(this@MainActivity, "Тренировка обновлена!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Ошибка при обновлении", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showMyWorkouts() {
        val trainer = FitnessDatabase.getTrainerByUserId(currentUser.id) ?: return
        val myWorkouts = FitnessDatabase.getWorkoutsByTrainer(trainer.id)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)

            val scrollView = ScrollView(this@MainActivity)
            val container = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
            }

            val title = TextView(this@MainActivity).apply {
                text = "📅 Мои тренировки"
                textSize = 20f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 16)
            }
            container.addView(title)

            if (myWorkouts.isEmpty()) {
                val emptyText = TextView(this@MainActivity).apply {
                    text = "У вас пока нет тренировок"
                    gravity = Gravity.CENTER
                    setPadding(0, 32, 0, 32)
                }
                container.addView(emptyText)
            } else {
                myWorkouts.sortedBy { it.date }.forEach { workout ->
                    val workoutReviews = FitnessDatabase.getWorkoutReviews(workout.id)
                    val avgRating = FitnessDatabase.getAverageWorkoutRating(workout.id)

                    val card = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(16, 16, 16, 16)
                        background = resources.getDrawable(android.R.drawable.dialog_holo_light_frame)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = 12 }

                        val titleText = TextView(this@MainActivity).apply {
                            text = workout.title
                            textSize = 18f
                            setTypeface(typeface, android.graphics.Typeface.BOLD)
                        }
                        addView(titleText)

                        val dateTime = TextView(this@MainActivity).apply {
                            text = "📅 ${workout.date} 🕒 ${workout.time}"
                            setPadding(0, 8, 0, 4)
                        }
                        addView(dateTime)

                        val participants = TextView(this@MainActivity).apply {
                            text = "👥 ${workout.currentParticipants}/${workout.maxParticipants} участников"
                            setPadding(0, 4, 0, 4)
                        }
                        addView(participants)

                        val rating = TextView(this@MainActivity).apply {
                            text = "⭐ Рейтинг: ${String.format("%.1f", avgRating)}/5.0 (${workoutReviews.size} отзывов)"
                            setPadding(0, 4, 0, 4)
                            setTextColor(resources.getColor(android.R.color.holo_orange_dark))
                        }
                        addView(rating)

                        val location = TextView(this@MainActivity).apply {
                            text = "📍 ${workout.location}"
                            setPadding(0, 4, 0, 4)
                        }
                        addView(location)

                        val price = TextView(this@MainActivity).apply {
                            text = "💰 ${workout.price} руб."
                            setTypeface(typeface, android.graphics.Typeface.BOLD)
                            setTextColor(resources.getColor(android.R.color.holo_green_dark))
                            setPadding(0, 4, 0, 4)
                        }
                        addView(price)

                        val status = TextView(this@MainActivity).apply {
                            text = if (workout.isAvailable) "✅ Доступна" else "❌ Заполнена"
                            setTextColor(
                                if (workout.isAvailable) resources.getColor(android.R.color.holo_green_dark)
                                else resources.getColor(android.R.color.holo_red_dark)
                            )
                        }
                        addView(status)

                        val buttonsLayout = LinearLayout(this@MainActivity).apply {
                            orientation = LinearLayout.HORIZONTAL
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { topMargin = 8 }
                        }

                        val btnEdit = Button(this@MainActivity).apply {
                            text = "✏️ Ред."
                            layoutParams = LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1f
                            ).apply { rightMargin = 4 }
                            setOnClickListener { showEditWorkoutDialog(workout) }
                        }
                        buttonsLayout.addView(btnEdit)

                        val btnDetails = Button(this@MainActivity).apply {
                            text = "👁️ Детали"
                            layoutParams = LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1f
                            ).apply { leftMargin = 4 }
                            setOnClickListener { showWorkoutDetails(workout, true) }
                        }
                        buttonsLayout.addView(btnDetails)

                        addView(buttonsLayout)
                    }
                    container.addView(card)
                }
            }

            val btnBack = Button(this@MainActivity).apply {
                text = "Назад"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 16 }
                setOnClickListener { showTrainerDashboard() }
            }
            container.addView(btnBack)

            scrollView.addView(container)
            addView(scrollView)
        }

        setContentView(layout)
    }

    private fun showTrainerSchedule() {
        val trainer = FitnessDatabase.getTrainerByUserId(currentUser.id) ?: return
        val myWorkouts = FitnessDatabase.getWorkoutsByTrainer(trainer.id)

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val upcomingWorkouts = myWorkouts.filter { it.date >= today && it.isActive }.sortedBy { it.date }

        val message = if (upcomingWorkouts.isEmpty()) {
            "У вас нет запланированных тренировок"
        } else {
            val builder = StringBuilder()
            builder.append("🗓️ РАСПИСАНИЕ ТРЕНИРОВОК\n\n")

            upcomingWorkouts.forEach { workout ->
                val workoutReviews = FitnessDatabase.getWorkoutReviews(workout.id)
                val avgRating = FitnessDatabase.getAverageWorkoutRating(workout.id)

                builder.append("🏋️ ${workout.title}\n")
                builder.append("   📅 ${workout.date} 🕒 ${workout.time}\n")
                builder.append("   ⏱️ ${workout.duration} мин.\n")
                builder.append("   👥 ${workout.currentParticipants}/${workout.maxParticipants}\n")
                builder.append("   📍 ${workout.location}\n")
                builder.append("   💰 ${workout.price} руб.\n")
                builder.append("   ⭐ ${String.format("%.1f", avgRating)}/5.0 (${workoutReviews.size} отзывов)\n\n")
            }

            builder.toString()
        }

        AlertDialog.Builder(this)
            .setTitle("🗓️ Мое расписание")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showMyClients() {
        val trainer = FitnessDatabase.getTrainerByUserId(currentUser.id) ?: return
        val myWorkouts = FitnessDatabase.getWorkoutsByTrainer(trainer.id)

        val clientIds = mutableSetOf<String>()
        myWorkouts.forEach { workout ->
            FitnessDatabase.bookings.filter { it.workoutId == workout.id && it.status == "confirmed" }
                .forEach { clientIds.add(it.clientId) }
        }

        val clients = FitnessDatabase.users.filter { it.id in clientIds }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)

            val scrollView = ScrollView(this@MainActivity)
            val container = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
            }

            val title = TextView(this@MainActivity).apply {
                text = "👥 Мои клиенты"
                textSize = 20f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 16)
            }
            container.addView(title)

            if (clients.isEmpty()) {
                val emptyText = TextView(this@MainActivity).apply {
                    text = "У вас пока нет клиентов"
                    gravity = Gravity.CENTER
                    setPadding(0, 32, 0, 32)
                }
                container.addView(emptyText)
            } else {
                clients.forEach { client ->
                    val clientWorkouts = myWorkouts.filter { workout ->
                        FitnessDatabase.bookings.any {
                            it.workoutId == workout.id && it.clientId == client.id && it.status == "confirmed"
                        }
                    }

                    val totalSpent = PaymentProcessor.getTotalSpent(client.id)

                    val card = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(16, 16, 16, 16)
                        background = resources.getDrawable(android.R.drawable.dialog_holo_light_frame)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = 12 }

                        // Фото клиента (если есть)
                        if (client.profilePhoto != null) {
                            val bitmap = PhotoHelper.base64ToBitmap(client.profilePhoto)
                            val imageView = ImageView(this@MainActivity).apply {
                                setImageBitmap(bitmap)
                                layoutParams = LinearLayout.LayoutParams(
                                    150, 150
                                ).apply {
                                    gravity = Gravity.CENTER
                                    bottomMargin = 8
                                }
                                scaleType = ImageView.ScaleType.CENTER_CROP
                            }
                            addView(imageView)
                        }

                        val name = TextView(this@MainActivity).apply {
                            text = "👤 ${client.name}"
                            textSize = 18f
                            setTypeface(typeface, android.graphics.Typeface.BOLD)
                        }
                        addView(name)

                        val contact = TextView(this@MainActivity).apply {
                            text = "📧 ${client.email}\n📱 ${client.phone}"
                            setPadding(0, 8, 0, 8)
                        }
                        addView(contact)

                        val stats = TextView(this@MainActivity).apply {
                            text = "📅 Записан на тренировок: ${clientWorkouts.size}\n💰 Потрачено: $totalSpent руб."
                            setPadding(0, 4, 0, 4)
                        }
                        addView(stats)

                        if (client.membershipFrozen) {
                            val frozenLabel = TextView(this@MainActivity).apply {
                                text = "❄️ Абонемент заморожен"
                                setTextColor(resources.getColor(android.R.color.holo_blue_dark))
                            }
                            addView(frozenLabel)
                        }
                    }
                    container.addView(card)
                }
            }

            val btnBack = Button(this@MainActivity).apply {
                text = "Назад"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 16 }
                setOnClickListener { showTrainerDashboard() }
            }
            container.addView(btnBack)

            scrollView.addView(container)
            addView(scrollView)
        }

        setContentView(layout)
    }

    private fun showEditProfileDialog() {
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)

            val etName = EditText(this@MainActivity).apply {
                setText(currentUser.name)
                hint = "Имя"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }
            addView(etName)

            val etEmail = EditText(this@MainActivity).apply {
                setText(currentUser.email)
                hint = "Email"
                inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }
            addView(etEmail)

            val etPhone = EditText(this@MainActivity).apply {
                setText(currentUser.phone)
                hint = "Телефон"
                inputType = InputType.TYPE_CLASS_PHONE
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16 }
            }
            addView(etPhone)

            val btnChoosePhoto = Button(this@MainActivity).apply {
                text = "📷 Выбрать фото"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16 }
                setOnClickListener {
                    getContent.launch("image/*")
                }
            }
            addView(btnChoosePhoto)

            if (tempProfilePhoto != null) {
                val photoHint = TextView(this@MainActivity).apply {
                    text = "✅ Фото выбрано"
                    setTextColor(resources.getColor(android.R.color.holo_green_dark))
                }
                addView(photoHint)
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Редактировать профиль")
            .setView(dialogLayout)
            .setPositiveButton("Сохранить") { dialog, which ->
                val name = (dialogLayout.getChildAt(0) as EditText).text.toString()
                val email = (dialogLayout.getChildAt(1) as EditText).text.toString()
                val phone = (dialogLayout.getChildAt(2) as EditText).text.toString()

                if (name.isNotEmpty()) {
                    val success = FitnessDatabase.updateUserProfile(
                        currentUser.id,
                        name,
                        email,
                        phone,
                        tempProfilePhoto
                    )

                    if (success) {
                        // Обновляем текущего пользователя
                        val updatedUser = FitnessDatabase.users.find { it.id == currentUser.id }
                        if (updatedUser != null) {
                            currentUser = updatedUser
                        }

                        tempProfilePhoto = null
                        Toast.makeText(this@MainActivity, "Профиль обновлен!", Toast.LENGTH_SHORT).show()
                        showDashboard()
                    } else {
                        Toast.makeText(this@MainActivity, "Ошибка при обновлении", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Имя не может быть пустым", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showTrainerProfile() {
        val trainer = FitnessDatabase.getTrainerByUserId(currentUser.id)

        val reviews = trainer?.let { FitnessDatabase.getReviewsForTrainer(it.id) } ?: emptyList()
        val avgRating = trainer?.let { FitnessDatabase.getAverageRating(it.id) } ?: 0.0

        val reviewsText = if (reviews.isEmpty()) {
            "Пока нет отзывов"
        } else {
            reviews.joinToString("\n\n") { review ->
                "⭐ ${review.rating}/5\n👤 ${review.clientName}\n💬 ${review.comment}\n📅 ${review.date}"
            }
        }

        val photoBitmap = currentUser.profilePhoto?.let { PhotoHelper.base64ToBitmap(it) }

        val message = """
            👤 МОЙ ПРОФИЛЬ ТРЕНЕРА
            
            Имя: ${trainer?.name ?: currentUser.name}
            Специализация: ${trainer?.specialization ?: "Не указана"}
            Опыт: ${trainer?.experience ?: 0} лет
            Рейтинг: ${String.format("%.1f", avgRating)}/5.0 (${reviews.size} отзывов)
            
            📝 О себе:
            ${trainer?.description ?: "Нет описания"}
            
            📞 Контакты:
            📧 ${currentUser.email}
            📱 ${currentUser.phone}
            
            📊 СТАТИСТИКА:
            • Создано тренировок: ${trainer?.let { FitnessDatabase.getWorkoutsByTrainer(it.id).size } ?: 0}
            • Активных клиентов: ${FitnessDatabase.bookings.count { booking ->
            val workout = FitnessDatabase.workouts.find { it.id == booking.workoutId }
            workout?.trainerId == trainer?.id && booking.status == "confirmed"
        }}
            
            ⭐ ОТЗЫВЫ:
            $reviewsText
        """.trimIndent()

        val builder = AlertDialog.Builder(this)
            .setTitle("Профиль тренера")
            .setMessage(message)
            .setPositiveButton("OK", null)

        if (photoBitmap != null) {
            val imageView = ImageView(this).apply {
                setImageBitmap(photoBitmap)
                layoutParams = LinearLayout.LayoutParams(200, 200)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            builder.setView(imageView)
        }

        builder.show()
    }

    private fun showClientDashboard() {
        val totalSpent = PaymentProcessor.getTotalSpent(currentUser.id)
        val (membership, purchaseDate, expiryDate) = FitnessDatabase.getUserMembershipInfo(currentUser.id)

        val today = Date()
        val isMembershipActive = if (expiryDate != null) {
            val expiry = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(expiryDate)
            expiry != null && expiry.after(today)
        } else false

        val daysLeft = if (isMembershipActive && expiryDate != null) {
            val expiry = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(expiryDate)
            val diff = expiry.time - today.time
            TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).toInt()
        } else 0

        val remainingFreezeDays = FitnessDatabase.getRemainingFreezeDays(currentUser.id)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)

            val title = TextView(this@MainActivity).apply {
                text = "💪 Фитнес-Клуб"
                textSize = 22f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 24)
            }
            addView(title)

            val welcome = TextView(this@MainActivity).apply {
                text = "Добро пожаловать, ${currentUser.name}!"
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 8)
            }
            addView(welcome)

            if (isMembershipActive && membership != null) {
                val membershipCard = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16, 16, 16, 16)
                    background = resources.getDrawable(android.R.drawable.dialog_holo_light_frame)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 16 }
                }

                membershipCard.addView(TextView(this@MainActivity).apply {
                    text = if (currentUser.membershipFrozen) "❄️ АБОНЕМЕНТ ЗАМОРОЖЕН" else "🎫 АКТИВНЫЙ АБОНЕМЕНТ"
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(if (currentUser.membershipFrozen) resources.getColor(android.R.color.holo_blue_dark) else resources.getColor(android.R.color.holo_green_dark))
                    gravity = Gravity.CENTER
                })

                membershipCard.addView(TextView(this@MainActivity).apply {
                    text = "${membership.name}"
                    textSize = 18f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    gravity = Gravity.CENTER
                    setPadding(0, 8, 0, 4)
                })

                if (!currentUser.membershipFrozen) {
                    membershipCard.addView(TextView(this@MainActivity).apply {
                        text = "Действует до: $expiryDate"
                        gravity = Gravity.CENTER
                    })

                    membershipCard.addView(TextView(this@MainActivity).apply {
                        text = "Осталось дней: $daysLeft"
                        gravity = Gravity.CENTER
                        setTextColor(resources.getColor(android.R.color.holo_orange_dark))
                        setPadding(0, 4, 0, 8)
                    })

                    membershipCard.addView(TextView(this@MainActivity).apply {
                        text = "❄️ Доступно дней заморозки: $remainingFreezeDays"
                        gravity = Gravity.CENTER
                        setTextColor(resources.getColor(android.R.color.holo_blue_dark))
                        setPadding(0, 4, 0, 8)
                    })

                    val buttonsLayout = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    val btnExtend = Button(this@MainActivity).apply {
                        text = "🔄 Продлить"
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        ).apply { rightMargin = 4 }
                        setOnClickListener { showExtendMembershipDialog() }
                    }
                    buttonsLayout.addView(btnExtend)

                    if (remainingFreezeDays > 0) {
                        val btnFreeze = Button(this@MainActivity).apply {
                            text = "❄️ Заморозить"
                            layoutParams = LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1f
                            ).apply { leftMargin = 4 }
                            setBackgroundColor(resources.getColor(android.R.color.holo_blue_dark))
                            setTextColor(resources.getColor(android.R.color.white))
                            setOnClickListener { showFreezeMembershipDialog() }
                        }
                        buttonsLayout.addView(btnFreeze)
                    }

                    membershipCard.addView(buttonsLayout)

                } else {
                    membershipCard.addView(TextView(this@MainActivity).apply {
                        text = "Заморожен с: ${currentUser.membershipFrozenDate}"
                        gravity = Gravity.CENTER
                    })

                    membershipCard.addView(TextView(this@MainActivity).apply {
                        text = "Дней заморозки: ${currentUser.membershipFrozenDays}"
                        gravity = Gravity.CENTER
                        setPadding(0, 4, 0, 8)
                    })

                    val btnUnfreeze = Button(this@MainActivity).apply {
                        text = "❄️ Разморозить"
                        setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
                        setTextColor(resources.getColor(android.R.color.white))
                        setOnClickListener { showUnfreezeMembershipDialog() }
                    }
                    membershipCard.addView(btnUnfreeze)
                }

                addView(membershipCard)
            } else {
                val noMembershipCard = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16, 16, 16, 16)
                    background = resources.getDrawable(android.R.drawable.dialog_holo_light_frame)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 16 }
                }

                noMembershipCard.addView(TextView(this@MainActivity).apply {
                    text = "❌ НЕТ АКТИВНОГО АБОНЕМЕНТА"
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(resources.getColor(android.R.color.holo_red_light))
                    gravity = Gravity.CENTER
                    setPadding(0, 0, 0, 8)
                })

                noMembershipCard.addView(TextView(this@MainActivity).apply {
                    text = "Приобретите абонемент для доступа к тренировкам"
                    gravity = Gravity.CENTER
                    textSize = 14f
                    setPadding(0, 0, 0, 8)
                })

                addView(noMembershipCard)
            }

            val balanceText = TextView(this@MainActivity).apply {
                text = "💰 Всего потрачено: $totalSpent руб."
                textSize = 16f
                gravity = Gravity.CENTER
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(resources.getColor(android.R.color.holo_green_dark))
                setPadding(0, 0, 0, 24)
            }
            addView(balanceText)

            val btnAvailableWorkouts = Button(this@MainActivity).apply {
                text = "🏋️ Доступные тренировки"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
                setOnClickListener { showAllWorkouts(false) }
            }
            addView(btnAvailableWorkouts)

            val btnViewTrainers = Button(this@MainActivity).apply {
                text = "👥 Наши тренеры"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
                setOnClickListener { showClientTrainers() }
            }
            addView(btnViewTrainers)

            val btnMyBookings = Button(this@MainActivity).apply {
                text = "📖 Мои записи"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
                setOnClickListener { showMyBookings() }
            }
            addView(btnMyBookings)

            val btnEditProfile = Button(this@MainActivity).apply {
                text = "✏️ Редактировать профиль"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
                setOnClickListener { showEditProfileDialog() }
            }
            addView(btnEditProfile)

            val btnProfile = Button(this@MainActivity).apply {
                text = "👤 Мой профиль"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
                setOnClickListener { showClientProfile() }
            }
            addView(btnProfile)

            val btnMemberships = Button(this@MainActivity).apply {
                text = "🎫 Абонементы"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
                setOnClickListener { showClientMemberships() }
            }
            addView(btnMemberships)

            val btnLogout = Button(this@MainActivity).apply {
                text = "🚪 Выйти"
                setBackgroundColor(resources.getColor(android.R.color.holo_red_light))
                setTextColor(resources.getColor(android.R.color.white))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 32 }
                setOnClickListener {
                    isLoggedIn = false
                    showLoginScreen()
                }
            }
            addView(btnLogout)
        }

        setContentView(layout)
    }

    private fun showFreezeMembershipDialog() {
        val remainingFreezeDays = FitnessDatabase.getRemainingFreezeDays(currentUser.id)

        // Создаем EditText для ввода количества дней
        val input = EditText(this@MainActivity).apply {
            hint = "Количество дней (1-$remainingFreezeDays)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        AlertDialog.Builder(this)
            .setTitle("❄️ Заморозка абонемента")
            .setMessage("""
                Вы можете заморозить абонемент на срок до 15 дней в год.
                
                Доступно дней заморозки: $remainingFreezeDays
                
                На сколько дней хотите заморозить?
            """.trimIndent())
            .setView(input)
            .setPositiveButton("Заморозить") { dialog, which ->
                val days = input.text.toString().toIntOrNull() ?: 0
                if (days in 1..remainingFreezeDays) {
                    val success = FitnessDatabase.freezeMembership(currentUser.id)
                    if (success) {
                        val updatedUser = FitnessDatabase.users.find { it.id == currentUser.id }
                        if (updatedUser != null) {
                            currentUser = updatedUser
                        }
                        Toast.makeText(this@MainActivity, "Абонемент заморожен на $days дней!", Toast.LENGTH_SHORT).show()
                        showClientDashboard()
                    } else {
                        Toast.makeText(this@MainActivity, "Ошибка при заморозке", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Введите корректное количество дней (1-$remainingFreezeDays)", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showUnfreezeMembershipDialog() {
        AlertDialog.Builder(this)
            .setTitle("❄️ Разморозка абонемента")
            .setMessage("Вы уверены, что хотите разморозить абонемент?")
            .setPositiveButton("Разморозить") { dialog, which ->
                val success = FitnessDatabase.unfreezeMembership(currentUser.id)
                if (success) {
                    val updatedUser = FitnessDatabase.users.find { it.id == currentUser.id }
                    if (updatedUser != null) {
                        currentUser = updatedUser
                    }
                    Toast.makeText(this@MainActivity, "Абонемент разморожен!", Toast.LENGTH_SHORT).show()
                    showClientDashboard()
                } else {
                    Toast.makeText(this@MainActivity, "Ошибка при разморозке", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showExtendMembershipDialog() {
        val (membership, purchaseDate, expiryDate) = FitnessDatabase.getUserMembershipInfo(currentUser.id)

        if (membership == null) {
            Toast.makeText(this@MainActivity, "У вас нет активного абонемента", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Продление абонемента")
            .setMessage("""
                Текущий абонемент: ${membership.name}
                Действует до: $expiryDate
                
                Стоимость продления: ${membership.price} руб.
                
                Продлить на ${membership.durationDays} дней?
            """.trimIndent())
            .setPositiveButton("Продлить") { dialog, which ->
                val methods = arrayOf("💳 Банковская карта", "📱 Apple Pay/Google Pay", "🏦 СБП")

                AlertDialog.Builder(this)
                    .setTitle("Выберите способ оплаты")
                    .setItems(methods) { d, w ->
                        val method = when(w) {
                            0 -> "card"
                            1 -> "mobile_pay"
                            else -> "sbp"
                        }

                        PaymentProcessor.processPayment(
                            userId = currentUser.id,
                            amount = membership.price,
                            method = method,
                            description = "Продление абонемента ${membership.name}"
                        )

                        val success = FitnessDatabase.extendMembership(currentUser.id)

                        if (success) {
                            // Обновляем текущего пользователя
                            val updatedUser = FitnessDatabase.users.find { it.id == currentUser.id }
                            if (updatedUser != null) {
                                currentUser = updatedUser
                            }

                            Toast.makeText(
                                this@MainActivity,
                                "Абонемент продлен до ${currentUser.membershipExpiryDate}!",
                                Toast.LENGTH_LONG
                            ).show()

                            showClientDashboard()
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Ошибка при продлении",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showClientMemberships() {
        val (currentMembership, purchaseDate, expiryDate) = FitnessDatabase.getUserMembershipInfo(currentUser.id)

        val today = Date()
        val isCurrentActive = if (expiryDate != null) {
            val expiry = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(expiryDate)
            expiry != null && expiry.after(today)
        } else false

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)

            val scrollView = ScrollView(this@MainActivity)
            val container = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
            }

            val title = TextView(this@MainActivity).apply {
                text = "🎫 Абонементы клуба"
                textSize = 20f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 16)
            }
            container.addView(title)

            FitnessDatabase.memberships.forEach { membership ->
                val card = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16, 16, 16, 16)
                    background = resources.getDrawable(android.R.drawable.dialog_holo_light_frame)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 12 }

                    val name = TextView(this@MainActivity).apply {
                        text = membership.name
                        textSize = 18f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                    }
                    addView(name)

                    val price = TextView(this@MainActivity).apply {
                        text = "💰 ${membership.price} руб."
                        textSize = 16f
                        setTextColor(resources.getColor(android.R.color.holo_green_dark))
                        setPadding(0, 8, 0, 4)
                    }
                    addView(price)

                    val details = TextView(this@MainActivity).apply {
                        text = "📅 ${membership.durationDays} дней\n🏋️ ${membership.workoutsCount} тренировок"
                        setPadding(0, 4, 0, 4)
                    }
                    addView(details)

                    val description = TextView(this@MainActivity).apply {
                        text = membership.description
                        setTextColor(resources.getColor(android.R.color.darker_gray))
                        setPadding(0, 8, 0, 8)
                    }
                    addView(description)

                    if (isCurrentActive && currentMembership?.id == membership.id) {
                        if (!currentUser.membershipFrozen) {
                            val activeLabel = TextView(this@MainActivity).apply {
                                text = "✅ ТЕКУЩИЙ АБОНЕМЕНТ"
                                setTextColor(resources.getColor(android.R.color.holo_green_dark))
                                setTypeface(typeface, android.graphics.Typeface.BOLD)
                                gravity = Gravity.CENTER
                            }
                            addView(activeLabel)

                            val btnExtend = Button(this@MainActivity).apply {
                                text = "🔄 Продлить"
                                setOnClickListener {
                                    showExtendMembershipDialog()
                                }
                            }
                            addView(btnExtend)
                        } else {
                            val frozenLabel = TextView(this@MainActivity).apply {
                                text = "❄️ ЗАМОРОЖЕН"
                                setTextColor(resources.getColor(android.R.color.holo_blue_dark))
                                setTypeface(typeface, android.graphics.Typeface.BOLD)
                                gravity = Gravity.CENTER
                            }
                            addView(frozenLabel)
                        }
                    } else {
                        val btnBuy = Button(this@MainActivity).apply {
                            text = "💳 Купить"
                            setBackgroundColor(resources.getColor(android.R.color.holo_blue_dark))
                            setTextColor(resources.getColor(android.R.color.white))
                            setOnClickListener {
                                showPurchaseMembershipDialog(membership)
                            }
                        }
                        addView(btnBuy)
                    }
                }
                container.addView(card)
            }

            val btnBack = Button(this@MainActivity).apply {
                text = "Назад"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 16 }
                setOnClickListener { showClientDashboard() }
            }
            container.addView(btnBack)

            scrollView.addView(container)
            addView(scrollView)
        }

        setContentView(layout)
    }

    private fun showUpgradePaymentDialog(membership: Membership, finalPrice: Int, currentMembership: Membership) {
        // Создаем layout для диалога
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        // Добавляем информацию о платеже
        dialogLayout.addView(TextView(this@MainActivity).apply {
            text = "Сумма к оплате: $finalPrice руб."
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        })

        // Создаем RadioGroup для выбора способа оплаты
        val radioGroup = RadioGroup(this@MainActivity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Добавляем варианты оплаты
        val cardRadio = RadioButton(this@MainActivity).apply {
            text = "💳 Банковская карта"
            isChecked = true
        }
        radioGroup.addView(cardRadio)

        val mobileRadio = RadioButton(this@MainActivity).apply {
            text = "📱 Apple Pay/Google Pay"
        }
        radioGroup.addView(mobileRadio)

        val sbpRadio = RadioButton(this@MainActivity).apply {
            text = "🏦 СБП"
        }
        radioGroup.addView(sbpRadio)

        dialogLayout.addView(radioGroup)

        // Создаем диалог с кнопками
        AlertDialog.Builder(this)
            .setTitle("Оплата апгрейда")
            .setView(dialogLayout)
            .setPositiveButton("Оплатить") { dialog, which ->
                // Определяем выбранный метод оплаты
                val selectedId = radioGroup.checkedRadioButtonId
                val method = when (selectedId) {
                    cardRadio.id -> "card"
                    mobileRadio.id -> "mobile_pay"
                    sbpRadio.id -> "sbp"
                    else -> "card"
                }

                if (finalPrice > 0) {
                    PaymentProcessor.processPayment(
                        userId = currentUser.id,
                        amount = finalPrice,
                        method = method,
                        description = "Апгрейд абонемента с ${currentMembership.name} на ${membership.name}"
                    )
                }

                val success = FitnessDatabase.purchaseMembership(currentUser.id, membership.id)

                if (success) {
                    val updatedUser = FitnessDatabase.users.find { it.id == currentUser.id }
                    if (updatedUser != null) {
                        currentUser = updatedUser
                    }

                    val message = if (finalPrice > 0) {
                        "Абонемент улучшен до ${membership.name}! Действует до ${currentUser.membershipExpiryDate}"
                    } else {
                        "Абонемент улучшен бесплатно! Действует до ${currentUser.membershipExpiryDate}"
                    }

                    Toast.makeText(
                        this@MainActivity,
                        message,
                        Toast.LENGTH_LONG
                    ).show()

                    showClientDashboard()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Ошибка при улучшении абонемента",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showPurchaseMembershipDialog(membership: Membership) {
        val (currentMembership, _, expiryDate) = FitnessDatabase.getUserMembershipInfo(currentUser.id)

        val today = Date()
        val isCurrentActive = if (expiryDate != null) {
            val expiry = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(expiryDate)
            expiry != null && expiry.after(today)
        } else false

        if (isCurrentActive && currentMembership != null) {
            if (membership.price > currentMembership.price) {
                // Апгрейд на более дорогой абонемент
                val dailyPrice = currentMembership.price / currentMembership.durationDays.toDouble()
                val expiry = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(expiryDate)
                val daysLeft = if (expiry != null) {
                    val diff = expiry.time - today.time
                    max(0, TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).toInt())
                } else 0

                val discount = (dailyPrice * daysLeft).toInt()
                val finalPrice = max(0, membership.price - discount)

                AlertDialog.Builder(this)
                    .setTitle("Апгрейд абонемента")
                    .setMessage("""
                        У вас есть активный абонемент "${currentMembership.name}" до $expiryDate.
                        
                        Осталось дней: $daysLeft
                        Скидка: $discount руб.
                        Итоговая цена: $finalPrice руб.
                        
                        Хотите перейти на "${membership.name}"?
                    """.trimIndent())
                    .setPositiveButton("Да, улучшить") { dialog, which ->
                        showUpgradePaymentDialog(membership, finalPrice, currentMembership)
                    }
                    .setNegativeButton("Отмена", null)
                    .show()

            } else if (membership.price < currentMembership.price) {
                // Даунгрейд на более дешевый абонемент
                AlertDialog.Builder(this)
                    .setTitle("Невозможно понизить абонемент")
                    .setMessage("""
                        У вас уже есть абонемент "${currentMembership.name}" с лучшими условиями.
                        
                        Вы не можете перейти на более дешевый абонемент, пока действует текущий.
                        
                        Дождитесь окончания действия текущего абонемента или продлите его.
                        
                        Текущий абонемент действует до: $expiryDate
                    """.trimIndent())
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                // Тот же абонемент - продление
                AlertDialog.Builder(this)
                    .setTitle("Продление абонемента")
                    .setMessage("""
                        У вас уже есть абонемент "${currentMembership.name}" до $expiryDate.
                        
                        Хотите продлить его на ${membership.durationDays} дней?
                        
                        Стоимость продления: ${membership.price} руб.
                    """.trimIndent())
                    .setPositiveButton("Продлить") { dialog, which ->
                        val methods = arrayOf("💳 Банковская карта", "📱 Apple Pay/Google Pay", "🏦 СБП")

                        AlertDialog.Builder(this)
                            .setTitle("Выберите способ оплаты")
                            .setItems(methods) { d, w ->
                                val method = when(w) {
                                    0 -> "card"
                                    1 -> "mobile_pay"
                                    else -> "sbp"
                                }

                                PaymentProcessor.processPayment(
                                    userId = currentUser.id,
                                    amount = membership.price,
                                    method = method,
                                    description = "Продление абонемента ${membership.name}"
                                )

                                val success = FitnessDatabase.extendMembership(currentUser.id)

                                if (success) {
                                    val updatedUser = FitnessDatabase.users.find { it.id == currentUser.id }
                                    if (updatedUser != null) {
                                        currentUser = updatedUser
                                    }

                                    Toast.makeText(
                                        this@MainActivity,
                                        "Абонемент продлен до ${currentUser.membershipExpiryDate}!",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    showClientDashboard()
                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Ошибка при продлении абонемента",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            .setNegativeButton("Отмена", null)
                            .show()
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
        } else {
            // Нет активного абонемента - обычная покупка
            val dialogLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 24, 24, 24)
            }

            dialogLayout.addView(TextView(this@MainActivity).apply {
                text = "Сумма к оплате: ${membership.price} руб."
                textSize = 18f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 24)
            })

            val radioGroup = RadioGroup(this@MainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val cardRadio = RadioButton(this@MainActivity).apply {
                text = "💳 Банковская карта"
                isChecked = true
            }
            radioGroup.addView(cardRadio)

            val mobileRadio = RadioButton(this@MainActivity).apply {
                text = "📱 Apple Pay/Google Pay"
            }
            radioGroup.addView(mobileRadio)

            val sbpRadio = RadioButton(this@MainActivity).apply {
                text = "🏦 СБП"
            }
            radioGroup.addView(sbpRadio)

            dialogLayout.addView(radioGroup)

            AlertDialog.Builder(this)
                .setTitle("Покупка абонемента")
                .setView(dialogLayout)
                .setPositiveButton("Оплатить") { dialog, which ->
                    val selectedId = radioGroup.checkedRadioButtonId
                    val method = when (selectedId) {
                        cardRadio.id -> "card"
                        mobileRadio.id -> "mobile_pay"
                        sbpRadio.id -> "sbp"
                        else -> "card"
                    }

                    PaymentProcessor.processPayment(
                        userId = currentUser.id,
                        amount = membership.price,
                        method = method,
                        description = "Абонемент ${membership.name}"
                    )

                    val success = FitnessDatabase.purchaseMembership(currentUser.id, membership.id)

                    if (success) {
                        val updatedUser = FitnessDatabase.users.find { it.id == currentUser.id }
                        if (updatedUser != null) {
                            currentUser = updatedUser
                        }

                        Toast.makeText(
                            this@MainActivity,
                            "Абонемент ${membership.name} приобретен! Действует до ${currentUser.membershipExpiryDate}",
                            Toast.LENGTH_LONG
                        ).show()

                        showClientDashboard()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Ошибка при покупке абонемента",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    private fun showClientTrainers() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)

            val scrollView = ScrollView(this@MainActivity)
            val container = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
            }

            val title = TextView(this@MainActivity).apply {
                text = "👥 Наши тренеры"
                textSize = 20f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 16)
            }
            container.addView(title)

            FitnessDatabase.trainers.forEach { trainer ->
                val reviews = FitnessDatabase.getReviewsForTrainer(trainer.id)
                val avgRating = FitnessDatabase.getAverageRating(trainer.id)

                val card = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16, 16, 16, 16)
                    background = resources.getDrawable(android.R.drawable.dialog_holo_light_frame)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 12 }

                    // Фото тренера (если есть)
                    val trainerUser = FitnessDatabase.users.find { it.id == trainer.userId }
                    if (trainerUser?.profilePhoto != null) {
                        val bitmap = PhotoHelper.base64ToBitmap(trainerUser.profilePhoto)
                        val imageView = ImageView(this@MainActivity).apply {
                            setImageBitmap(bitmap)
                            layoutParams = LinearLayout.LayoutParams(
                                200, 200
                            ).apply {
                                gravity = Gravity.CENTER
                                bottomMargin = 8
                            }
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                        addView(imageView)
                    }

                    val name = TextView(this@MainActivity).apply {
                        text = trainer.name
                        textSize = 18f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                    }
                    addView(name)

                    val specialization = TextView(this@MainActivity).apply {
                        text = "🏆 ${trainer.specialization}"
                        setPadding(0, 8, 0, 4)
                    }
                    addView(specialization)

                    val experience = TextView(this@MainActivity).apply {
                        text = "⏱️ Опыт: ${trainer.experience} лет"
                        setPadding(0, 4, 0, 4)
                    }
                    addView(experience)

                    val rating = TextView(this@MainActivity).apply {
                        text = "⭐ ${String.format("%.1f", avgRating)}/5.0 (${reviews.size} отзывов)"
                        setPadding(0, 4, 0, 8)
                        setTextColor(resources.getColor(android.R.color.holo_orange_dark))
                    }
                    addView(rating)

                    val btnDetails = Button(this@MainActivity).apply {
                        text = "Подробнее"
                        setOnClickListener { showTrainerDetails(trainer, false) }
                    }
                    addView(btnDetails)
                }
                container.addView(card)
            }

            val btnBack = Button(this@MainActivity).apply {
                text = "Назад"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 16 }
                setOnClickListener { showClientDashboard() }
            }
            container.addView(btnBack)

            scrollView.addView(container)
            addView(scrollView)
        }

        setContentView(layout)
    }

    private fun showAllWorkouts(isAdmin: Boolean = false) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)

            val scrollView = ScrollView(this@MainActivity)
            val container = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
            }

            val title = TextView(this@MainActivity).apply {
                text = if (isAdmin) "📅 Все тренировки" else "🏋️ Доступные тренировки"
                textSize = 20f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 16)
            }
            container.addView(title)

            val workouts = if (isAdmin) FitnessDatabase.workouts.filter { it.isActive } else FitnessDatabase.getAvailableWorkouts()

            if (workouts.isEmpty()) {
                val emptyText = TextView(this@MainActivity).apply {
                    text = "Нет доступных тренировок"
                    gravity = Gravity.CENTER
                    setPadding(0, 32, 0, 32)
                }
                container.addView(emptyText)
            } else {
                workouts.sortedBy { it.date }.forEach { workout ->
                    val trainer = FitnessDatabase.trainers.find { it.id == workout.trainerId }
                    val workoutReviews = FitnessDatabase.getWorkoutReviews(workout.id)
                    val avgRating = FitnessDatabase.getAverageWorkoutRating(workout.id)

                    val card = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(16, 16, 16, 16)
                        background = resources.getDrawable(android.R.drawable.dialog_holo_light_frame)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = 12 }

                        val titleText = TextView(this@MainActivity).apply {
                            text = workout.title
                            textSize = 18f
                            setTypeface(typeface, android.graphics.Typeface.BOLD)
                        }
                        addView(titleText)

                        val trainerText = TextView(this@MainActivity).apply {
                            text = "Тренер: ${trainer?.name ?: "Неизвестно"}"
                            setPadding(0, 8, 0, 4)
                        }
                        addView(trainerText)

                        val ratingText = TextView(this@MainActivity).apply {
                            text = "⭐ Рейтинг: ${String.format("%.1f", avgRating)}/5.0 (${workoutReviews.size} отзывов)"
                            setPadding(0, 4, 0, 4)
                            setTextColor(resources.getColor(android.R.color.holo_orange_dark))
                        }
                        addView(ratingText)

                        val dateTime = TextView(this@MainActivity).apply {
                            text = "📅 ${workout.date} 🕒 ${workout.time} (${workout.duration} мин.)"
                            setPadding(0, 4, 0, 4)
                        }
                        addView(dateTime)

                        val location = TextView(this@MainActivity).apply {
                            text = "📍 ${workout.location}"
                            setPadding(0, 4, 0, 4)
                        }
                        addView(location)

                        val participants = TextView(this@MainActivity).apply {
                            text = "👥 ${workout.currentParticipants}/${workout.maxParticipants} участников"
                            setPadding(0, 4, 0, 4)
                        }
                        addView(participants)

                        val price = TextView(this@MainActivity).apply {
                            text = "💰 ${workout.price} руб."
                            setTypeface(typeface, android.graphics.Typeface.BOLD)
                            setTextColor(resources.getColor(android.R.color.holo_green_dark))
                            setPadding(0, 4, 0, 4)
                        }
                        addView(price)

                        val btnDetails = Button(this@MainActivity).apply {
                            text = "Подробнее"
                            setOnClickListener { showWorkoutDetails(workout, isAdmin) }
                        }
                        addView(btnDetails)
                    }
                    container.addView(card)
                }
            }

            val btnBack = Button(this@MainActivity).apply {
                text = "Назад"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 16 }
                setOnClickListener {
                    if (isAdmin) showAdminDashboard() else showClientDashboard()
                }
            }
            container.addView(btnBack)

            scrollView.addView(container)
            addView(scrollView)
        }

        setContentView(layout)
    }

    private fun showMyBookings() {
        val myBookings = FitnessDatabase.getBookingsByClient(currentUser.id)

        Log.d("MY_BOOKINGS", "Всего бронирований в БД: ${FitnessDatabase.bookings.size}")
        Log.d("MY_BOOKINGS", "Бронирований для клиента ${currentUser.id}: ${myBookings.size}")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)

            val scrollView = ScrollView(this@MainActivity)
            val container = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
            }

            val title = TextView(this@MainActivity).apply {
                text = "📖 Мои записи"
                textSize = 20f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 16)
            }
            container.addView(title)

            if (myBookings.isEmpty()) {
                val emptyText = TextView(this@MainActivity).apply {
                    text = "У вас нет активных записей"
                    gravity = Gravity.CENTER
                    setPadding(0, 32, 0, 32)
                }
                container.addView(emptyText)
            } else {
                myBookings.forEach { booking ->
                    val workout = FitnessDatabase.workouts.find { it.id == booking.workoutId }
                    val trainer = workout?.let { FitnessDatabase.trainers.find { t -> t.id == it.trainerId } }
                    val workoutReviews = workout?.let { FitnessDatabase.getWorkoutReviews(it.id) } ?: emptyList()
                    val avgRating = workout?.let { FitnessDatabase.getAverageWorkoutRating(it.id) } ?: 0.0

                    if (workout != null) {
                        val card = LinearLayout(this@MainActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(16, 16, 16, 16)
                            background = resources.getDrawable(android.R.drawable.dialog_holo_light_frame)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { bottomMargin = 12 }

                            val titleText = TextView(this@MainActivity).apply {
                                text = workout.title
                                textSize = 18f
                                setTypeface(typeface, android.graphics.Typeface.BOLD)
                            }
                            addView(titleText)

                            val trainerText = TextView(this@MainActivity).apply {
                                text = "Тренер: ${trainer?.name ?: "Неизвестно"}"
                                setPadding(0, 8, 0, 4)
                            }
                            addView(trainerText)

                            val ratingText = TextView(this@MainActivity).apply {
                                text = "⭐ Рейтинг тренировки: ${String.format("%.1f", avgRating)}/5.0"
                                setPadding(0, 4, 0, 4)
                                setTextColor(resources.getColor(android.R.color.holo_orange_dark))
                            }
                            addView(ratingText)

                            val dateTime = TextView(this@MainActivity).apply {
                                text = "📅 ${workout.date} 🕒 ${workout.time}"
                                setPadding(0, 4, 0, 4)
                            }
                            addView(dateTime)

                            val location = TextView(this@MainActivity).apply {
                                text = "📍 ${workout.location}"
                                setPadding(0, 4, 0, 4)
                            }
                            addView(location)

                            val price = TextView(this@MainActivity).apply {
                                text = "💰 ${workout.price} руб."
                                setTypeface(typeface, android.graphics.Typeface.BOLD)
                                setTextColor(resources.getColor(android.R.color.holo_green_dark))
                                setPadding(0, 4, 0, 4)
                            }
                            addView(price)

                            val status = TextView(this@MainActivity).apply {
                                text = "✅ Подтверждено"
                                setTextColor(resources.getColor(android.R.color.holo_green_dark))
                                setPadding(0, 4, 0, 4)
                            }
                            addView(status)

                            val buttonsLayout = LinearLayout(this@MainActivity).apply {
                                orientation = LinearLayout.VERTICAL
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                ).apply { topMargin = 8 }
                            }

                            val btnCancel = Button(this@MainActivity).apply {
                                text = "❌ Отменить запись"
                                setBackgroundColor(resources.getColor(android.R.color.holo_red_light))
                                setTextColor(resources.getColor(android.R.color.white))
                                setOnClickListener {
                                    FitnessDatabase.cancelBooking(booking.id)
                                    Toast.makeText(this@MainActivity, "Запись отменена", Toast.LENGTH_SHORT).show()
                                    showMyBookings()
                                }
                            }
                            buttonsLayout.addView(btnCancel)

                            val hasReviewed = workoutReviews.any { it.clientId == currentUser.id }

                            if (!hasReviewed) {
                                val btnLeaveReview = Button(this@MainActivity).apply {
                                    text = "⭐ Оставить отзыв о тренировке"
                                    setOnClickListener { showLeaveWorkoutReviewDialog(workout) }
                                }
                                buttonsLayout.addView(btnLeaveReview)
                            }

                            addView(buttonsLayout)
                        }
                        container.addView(card)
                    }
                }
            }

            val btnBack = Button(this@MainActivity).apply {
                text = "Назад"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 16 }
                setOnClickListener { showClientDashboard() }
            }
            container.addView(btnBack)

            scrollView.addView(container)
            addView(scrollView)
        }

        setContentView(layout)
    }

    private fun showLeaveWorkoutReviewDialog(workout: Workout) {
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)

            val ratingBar = RatingBar(this@MainActivity).apply {
                numStars = 5
                stepSize = 0.5f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                    bottomMargin = 16
                }
            }
            addView(ratingBar)

            val etComment = EditText(this@MainActivity).apply {
                hint = "Ваш отзыв о тренировке"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            addView(etComment)
        }

        AlertDialog.Builder(this)
            .setTitle("Оставить отзыв о тренировке")
            .setView(dialogLayout)
            .setPositiveButton("Отправить") { dialog, which ->
                val rating = (dialogLayout.getChildAt(0) as RatingBar).rating
                val comment = (dialogLayout.getChildAt(1) as EditText).text.toString()

                if (comment.isNotEmpty()) {
                    val review = WorkoutReview(
                        workoutId = workout.id,
                        clientId = currentUser.id,
                        clientName = currentUser.name,
                        rating = rating.toInt(),
                        comment = comment
                    )

                    FitnessDatabase.workoutReviews.add(review)
                    FitnessDatabase.saveAllData()

                    Toast.makeText(this@MainActivity, "Спасибо за отзыв о тренировке!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Напишите комментарий", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showLeaveReviewDialog(trainer: Trainer?) {
        if (trainer == null) {
            Toast.makeText(this@MainActivity, "Тренер не найден", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)

            val ratingBar = RatingBar(this@MainActivity).apply {
                numStars = 5
                stepSize = 0.5f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                    bottomMargin = 16
                }
            }
            addView(ratingBar)

            val etComment = EditText(this@MainActivity).apply {
                hint = "Ваш отзыв о тренере"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            addView(etComment)
        }

        AlertDialog.Builder(this)
            .setTitle("Оставить отзыв о ${trainer.name}")
            .setView(dialogLayout)
            .setPositiveButton("Отправить") { dialog, which ->
                val rating = (dialogLayout.getChildAt(0) as RatingBar).rating
                val comment = (dialogLayout.getChildAt(1) as EditText).text.toString()

                if (comment.isNotEmpty()) {
                    val review = Review(
                        trainerId = trainer.id,
                        clientId = currentUser.id,
                        clientName = currentUser.name,
                        rating = rating.toInt(),
                        comment = comment
                    )

                    FitnessDatabase.reviews.add(review)

                    val avgRating = FitnessDatabase.getAverageRating(trainer.id)
                    val trainerIndex = FitnessDatabase.trainers.indexOf(trainer)
                    if (trainerIndex != -1) {
                        FitnessDatabase.trainers[trainerIndex] = trainer.copy(
                            rating = avgRating
                        )
                    }

                    FitnessDatabase.saveAllData()

                    Toast.makeText(this@MainActivity, "Спасибо за отзыв о тренере!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Напишите комментарий", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showClientProfile() {
        val myBookings = FitnessDatabase.getBookingsByClient(currentUser.id)
        val totalSpent = PaymentProcessor.getTotalSpent(currentUser.id)
        val (membership, purchaseDate, expiryDate) = FitnessDatabase.getUserMembershipInfo(currentUser.id)
        val remainingFreezeDays = FitnessDatabase.getRemainingFreezeDays(currentUser.id)

        val today = Date()
        val isMembershipActive = if (expiryDate != null) {
            val expiry = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(expiryDate)
            expiry != null && expiry.after(today)
        } else false

        val daysLeft = if (isMembershipActive && expiryDate != null) {
            val expiry = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(expiryDate)
            val diff = expiry.time - today.time
            TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).toInt()
        } else 0

        val photoBitmap = currentUser.profilePhoto?.let { PhotoHelper.base64ToBitmap(it) }

        val message = """
            👤 МОЙ ПРОФИЛЬ
            
            Имя: ${currentUser.name}
            Email: ${currentUser.email}
            Телефон: ${currentUser.phone}
            
            🎫 Абонемент:
            ${membership?.name ?: "Не выбран"}
            ${if (isMembershipActive) "✅ Активен до: $expiryDate (осталось $daysLeft дн.)" else "❌ Не активен"}
            ${if (currentUser.membershipFrozen) "❄️ Заморожен с: ${currentUser.membershipFrozenDate}" else ""}
            Дата покупки: ${purchaseDate ?: "-"}
            
            ❄️ Доступно дней заморозки: $remainingFreezeDays
            
            📊 МОЯ СТАТИСТИКА:
            • Активных записей: ${myBookings.size}
            • Всего тренировок: ${FitnessDatabase.bookings.count { it.clientId == currentUser.id }}
            • Потрачено всего: $totalSpent руб.
            
            🎯 МОИ ЦЕЛИ:
            • Посетить еще ${5 - myBookings.size} тренировок
            • Попробовать разные направления
            
            💪 РЕКОМЕНДАЦИИ:
            ${if (myBookings.size < 3) "Начните с 2-3 тренировок в неделю"
        else "Отличный прогресс! Продолжайте в том же темпе!"}
        """.trimIndent()

        val builder = AlertDialog.Builder(this)
            .setTitle("Мой профиль")
            .setMessage(message)
            .setPositiveButton("OK", null)

        if (photoBitmap != null) {
            val imageView = ImageView(this).apply {
                setImageBitmap(photoBitmap)
                layoutParams = LinearLayout.LayoutParams(200, 200)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            builder.setView(imageView)
        }

        builder.show()
    }

    private fun showTrainerDetails(trainer: Trainer, isAdmin: Boolean) {
        val trainerWorkouts = FitnessDatabase.workouts.filter { it.trainerId == trainer.id && it.isActive }
        val availableWorkouts = trainerWorkouts.filter { it.isAvailable && it.currentParticipants < it.maxParticipants }
        val reviews = FitnessDatabase.getReviewsForTrainer(trainer.id)
        val avgRating = FitnessDatabase.getAverageRating(trainer.id)

        val reviewsText = if (reviews.isEmpty()) {
            "Пока нет отзывов"
        } else {
            reviews.takeLast(3).joinToString("\n\n") { review ->
                "⭐ ${review.rating}/5 - ${review.clientName}\n💬 ${review.comment}"
            }
        }

        val trainerUser = FitnessDatabase.users.find { it.id == trainer.userId }
        val photoBitmap = trainerUser?.profilePhoto?.let { PhotoHelper.base64ToBitmap(it) }

        val message = """
            👤 ${trainer.name}
            
            🏆 Специализация:
            ${trainer.specialization}
            
            ⏱️ Опыт работы:
            ${trainer.experience} лет
            
            ⭐ Рейтинг:
            ${String.format("%.1f", avgRating)}/5.0 (${reviews.size} отзывов)
            
            📝 Описание:
            ${trainer.description}
            
            📅 Доступные тренировки:
            ${if (availableWorkouts.isEmpty()) "Нет доступных тренировок"
        else availableWorkouts.joinToString("\n") { "• ${it.title} (${it.date} ${it.time})" }}
            
            📊 Статистика:
            • Всего тренировок: ${trainerWorkouts.size}
            • Записано клиентов: ${trainerWorkouts.sumOf { it.currentParticipants }}
            
            ⭐ Последние отзывы:
            $reviewsText
        """.trimIndent()

        val builder = AlertDialog.Builder(this)
            .setTitle("👤 Информация о тренере")
            .setMessage(message)
            .setPositiveButton("OK", null)

        if (photoBitmap != null) {
            val imageView = ImageView(this).apply {
                setImageBitmap(photoBitmap)
                layoutParams = LinearLayout.LayoutParams(200, 200)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            builder.setView(imageView)
        }

        if (isAdmin) {
            builder.setNeutralButton("Удалить тренера") { dialog, which ->
                FitnessDatabase.users.removeIf { it.id == trainer.userId }
                FitnessDatabase.trainers.remove(trainer)
                FitnessDatabase.workouts.removeAll { it.trainerId == trainer.id }
                FitnessDatabase.saveAllData()
                Toast.makeText(this@MainActivity, "Тренер удален", Toast.LENGTH_SHORT).show()
                showAllTrainers()
            }
        } else if (currentUser.role == "client") {
            builder.setNeutralButton("Оставить отзыв") { dialog, which ->
                showLeaveReviewDialog(trainer)
            }
        }

        builder.show()
    }

    private fun showWorkoutDetails(workout: Workout, isTrainerOrAdmin: Boolean) {
        val trainer = FitnessDatabase.trainers.find { it.id == workout.trainerId }
        val isClient = currentUser.role == "client"
        val alreadyBooked = FitnessDatabase.bookings.any {
            it.workoutId == workout.id && it.clientId == currentUser.id && it.status == "confirmed"
        }
        val trainerReviews = trainer?.let { FitnessDatabase.getReviewsForTrainer(it.id) } ?: emptyList()
        val trainerAvgRating = trainer?.let { FitnessDatabase.getAverageRating(it.id) } ?: 0.0
        val workoutReviews = FitnessDatabase.getWorkoutReviews(workout.id)
        val workoutAvgRating = FitnessDatabase.getAverageWorkoutRating(workout.id)

        val hasReviewedWorkout = workoutReviews.any { it.clientId == currentUser.id }

        val message = """
            🏋️ ${workout.title}
            
            👤 Тренер: ${trainer?.name ?: "Неизвестно"}
            ⭐ Рейтинг тренера: ${String.format("%.1f", trainerAvgRating)}/5.0 (${trainerReviews.size} отзывов)
            
            📝 Описание:
            ${workout.description}
            
            📅 Дата и время:
            ${workout.date} ${workout.time}
            
            ⏱️ Длительность:
            ${workout.duration} минут
            
            📍 Место:
            ${workout.location}
            
            👥 Участники:
            ${workout.currentParticipants}/${workout.maxParticipants}
            
            💰 Цена:
            ${workout.price} руб.
            
            ⭐ Рейтинг тренировки:
            ${String.format("%.1f", workoutAvgRating)}/5.0 (${workoutReviews.size} отзывов)
            
            ${if (!workout.isAvailable) "❌ Тренировка заполнена"
        else if (alreadyBooked) "✅ Вы уже записаны"
        else "✅ Доступна для записи"}
        """.trimIndent()

        val builder = AlertDialog.Builder(this)
            .setTitle("🏋️ Детали тренировки")
            .setMessage(message)

        if (isClient && workout.isAvailable && !alreadyBooked) {
            builder.setPositiveButton("💳 Записаться и оплатить") { dialog, which ->
                showPaymentForWorkoutDialog(workout, trainer)
            }
            builder.setNeutralButton("👤 О тренере") { dialog, which ->
                trainer?.let { showTrainerDetails(it, false) }
            }
            builder.setNegativeButton("Отмена", null)
        } else if (isTrainerOrAdmin && currentUser.role == "trainer" && workout.trainerId == trainer?.id) {
            builder.setPositiveButton("OK", null)
            builder.setNeutralButton("❌ Отменить тренировку") { dialog, which ->
                AlertDialog.Builder(this)
                    .setTitle("Подтверждение")
                    .setMessage("Вы уверены, что хотите отменить эту тренировку? Все клиенты будут уведомлены.")
                    .setPositiveButton("Да, отменить") { d, w ->
                        FitnessDatabase.cancelWorkout(workout.id)
                        Toast.makeText(this@MainActivity, "Тренировка отменена", Toast.LENGTH_SHORT).show()
                        showMyWorkouts()
                    }
                    .setNegativeButton("Нет", null)
                    .show()
            }
        } else if (isTrainerOrAdmin && currentUser.role == "admin") {
            builder.setPositiveButton("OK", null)
            builder.setNeutralButton("Удалить тренировку") { dialog, which ->
                FitnessDatabase.workouts.removeIf { it.id == workout.id }
                FitnessDatabase.saveAllData()
                Toast.makeText(this@MainActivity, "Тренировка удалена", Toast.LENGTH_SHORT).show()
                showAllWorkouts(true)
            }
        } else {
            builder.setPositiveButton("OK", null)
            if (isClient && alreadyBooked && !hasReviewedWorkout) {
                builder.setNeutralButton("⭐ Оставить отзыв") { dialog, which ->
                    showLeaveWorkoutReviewDialog(workout)
                }
            } else if (isClient && alreadyBooked) {
                builder.setNeutralButton("❌ Отменить запись") { dialog, which ->
                    val booking = FitnessDatabase.bookings.find {
                        it.workoutId == workout.id && it.clientId == currentUser.id && it.status == "confirmed"
                    }
                    booking?.let {
                        FitnessDatabase.cancelBooking(it.id)
                        Toast.makeText(this@MainActivity, "Запись отменена", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        builder.show()
    }

    private fun showPaymentForWorkoutDialog(workout: Workout, trainer: Trainer?) {
        val (membership, _, expiryDate) = FitnessDatabase.getUserMembershipInfo(currentUser.id)

        val today = Date()
        val isMembershipActive = if (expiryDate != null) {
            val expiry = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(expiryDate)
            expiry != null && expiry.after(today) && !currentUser.membershipFrozen
        } else false

        if (isMembershipActive) {
            AlertDialog.Builder(this)
                .setTitle("Оплата тренировки")
                .setMessage("У вас есть активный абонемент. Хотите использовать его для этой тренировки?")
                .setPositiveButton("Да, использовать абонемент") { dialog, which ->
                    // Используем абонемент (бесплатно)
                    val success = FitnessDatabase.bookWorkout(workout.id, currentUser.id)

                    if (success) {
                        Toast.makeText(
                            this@MainActivity,
                            "Вы записаны на тренировку с использованием абонемента!",
                            Toast.LENGTH_LONG
                        ).show()

                        // Обновляем данные пользователя
                        val updatedUser = FitnessDatabase.users.find { it.id == currentUser.id }
                        if (updatedUser != null) {
                            currentUser = updatedUser
                        }
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Ошибка при записи. Возможно, тренировка уже заполнена.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                .setNeutralButton("Оплатить отдельно") { dialog, which ->
                    showPaymentMethodsDialog(workout, trainer)
                }
                .setNegativeButton("Отмена", null)
                .show()
        } else {
            showPaymentMethodsDialog(workout, trainer)
        }
    }

    private fun showPaymentMethodsDialog(workout: Workout, trainer: Trainer?) {
        val methods = arrayOf("💳 Банковская карта", "📱 Apple Pay/Google Pay", "🏦 СБП", "💰 Наличные в клубе")

        AlertDialog.Builder(this)
            .setTitle("Оплата тренировки")
            .setMessage("Тренировка: ${workout.title}\nТренер: ${trainer?.name}\nСумма: ${workout.price} руб.")
            .setItems(methods) { dialog, which ->
                val method = when(which) {
                    0 -> "card"
                    1 -> "mobile_pay"
                    2 -> "sbp"
                    else -> "cash"
                }

                // Сначала записываем на тренировку
                val success = FitnessDatabase.bookWorkout(workout.id, currentUser.id)

                if (success) {
                    // Если запись успешна, проводим оплату
                    PaymentProcessor.processPayment(
                        userId = currentUser.id,
                        amount = workout.price,
                        method = method,
                        description = "Тренировка: ${workout.title}"
                    )

                    Toast.makeText(
                        this@MainActivity,
                        "Оплата прошла успешно! Вы записаны на тренировку.",
                        Toast.LENGTH_LONG
                    ).show()

                    // Обновляем текущего пользователя
                    val updatedUser = FitnessDatabase.users.find { it.id == currentUser.id }
                    if (updatedUser != null) {
                        currentUser = updatedUser
                    }

                    // Проверяем, что запись создалась
                    val bookings = FitnessDatabase.getBookingsByClient(currentUser.id)
                    Log.d("PAYMENT", "После оплаты у клиента ${bookings.size} записей")

                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Ошибка при записи. Возможно, тренировка уже заполнена.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}

// ==================== КЛАСС ДЛЯ ПОЛУЧЕНИЯ УВЕДОМЛЕНИЙ ====================
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Напоминание"
        val message = intent.getStringExtra("message") ?: "У вас скоро тренировка"

        NotificationHelper.sendNotification(context, title, message)
    }
}