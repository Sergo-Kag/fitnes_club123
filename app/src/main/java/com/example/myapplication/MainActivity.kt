package com.example.fitnessclub

import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

// ==================== МОДЕЛИ ДАННЫХ ====================
data class User(
    val id: String = UUID.randomUUID().toString(),
    val username: String,
    val password: String,
    val role: String, // "admin", "trainer", "client"
    val name: String,
    val email: String = "",
    val phone: String = ""
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
    val price: Int = 0
)

data class Booking(
    val id: String = UUID.randomUUID().toString(),
    val workoutId: String,
    val clientId: String,
    val bookingDate: String,
    var status: String = "confirmed" // "confirmed", "cancelled", "completed"
)

// ==================== БАЗА ДАННЫХ ====================
object FitnessDatabase {
    val users = mutableListOf<User>()
    val trainers = mutableListOf<Trainer>()
    val workouts = mutableListOf<Workout>()
    val bookings = mutableListOf<Booking>()

    init {
        // Инициализация начальных данных
        initializeData()
    }

    private fun initializeData() {
        // Администратор
        val admin = User(
            id = "admin_001",
            username = "admin",
            password = "admin123",
            role = "admin",
            name = "Александр Иванов",
            email = "admin@fitnessclub.ru",
            phone = "+7 (999) 123-45-67"
        )

        // Тренеры
        val trainer1 = User(
            id = "trainer_001",
            username = "trainer1",
            password = "trainer123",
            role = "trainer",
            name = "Иван Петров",
            email = "ivan@fitnessclub.ru",
            phone = "+7 (999) 234-56-78"
        )

        val trainer2 = User(
            id = "trainer_002",
            username = "trainer2",
            password = "trainer123",
            role = "trainer",
            name = "Мария Сидорова",
            email = "maria@fitnessclub.ru",
            phone = "+7 (999) 345-67-89"
        )

        // Клиенты
        val client1 = User(
            id = "client_001",
            username = "client1",
            password = "client123",
            role = "client",
            name = "Алексей Смирнов",
            email = "alex@email.ru",
            phone = "+7 (999) 456-78-90"
        )

        users.addAll(listOf(admin, trainer1, trainer2, client1))

        // Профили тренеров
        trainers.addAll(listOf(
            Trainer(
                id = "t001",
                userId = trainer1.id,
                name = "Иван Петров",
                specialization = "Силовые тренировки, Бодибилдинг",
                experience = 8,
                rating = 4.9,
                description = "Сертифицированный тренер с 8-летним опытом. Специализация: силовые тренировки, набор мышечной массы, коррекция осанки."
            ),
            Trainer(
                id = "t002",
                userId = trainer2.id,
                name = "Мария Сидорова",
                specialization = "Йога, Пилатес, Стретчинг",
                experience = 6,
                rating = 4.8,
                description = "Инструктор групповых программ. Специализация: йога для начинающих, пилатес, стретчинг, женский фитнес."
            )
        ))

        // Тренировки
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        workouts.addAll(listOf(
            Workout(
                id = "w001",
                trainerId = "t001",
                title = "Силовая тренировка",
                description = "Проработка всех групп мышц с использованием свободных весов",
                date = today,
                time = "18:00",
                duration = 90,
                maxParticipants = 10,
                currentParticipants = 7,
                location = "Зал тяжелой атлетики",
                price = 800
            ),
            Workout(
                id = "w002",
                trainerId = "t001",
                title = "Функциональный тренинг",
                description = "Развитие силы, выносливости и координации",
                date = today,
                time = "20:00",
                duration = 60,
                maxParticipants = 15,
                currentParticipants = 12,
                location = "Функциональный зал",
                price = 600
            ),
            Workout(
                id = "w003",
                trainerId = "t002",
                title = "Утренняя йога",
                description = "Йога для пробуждения и заряда энергии на весь день",
                date = today,
                time = "07:00",
                duration = 75,
                maxParticipants = 20,
                currentParticipants = 15,
                location = "Зал групповых программ",
                price = 500
            ),
            Workout(
                id = "w004",
                trainerId = "t002",
                title = "Пилатес",
                description = "Укрепление мышц кора, улучшение гибкости и осанки",
                date = today,
                time = "19:00",
                duration = 60,
                maxParticipants = 12,
                currentParticipants = 9,
                location = "Зал пилатеса",
                price = 700
            )
        ))

        // Бронирования
        bookings.add(
            Booking(
                id = "b001",
                workoutId = "w001",
                clientId = client1.id,
                bookingDate = today,
                status = "confirmed"
            )
        )
    }

    // Вспомогательные методы
    fun getUserByCredentials(username: String, password: String): User? {
        return users.find { it.username == username && it.password == password }
    }

    fun getTrainerByUserId(userId: String): Trainer? {
        return trainers.find { it.userId == userId }
    }

    fun getWorkoutsByTrainer(trainerId: String): List<Workout> {
        return workouts.filter { it.trainerId == trainerId }
    }

    fun getAvailableWorkouts(): List<Workout> {
        return workouts.filter { it.isAvailable && it.currentParticipants < it.maxParticipants }
    }

    fun getBookingsByClient(clientId: String): List<Booking> {
        return bookings.filter { it.clientId == clientId && it.status == "confirmed" }
    }

    fun bookWorkout(workoutId: String, clientId: String): Boolean {
        val workout = workouts.find { it.id == workoutId }
        return if (workout != null && workout.currentParticipants < workout.maxParticipants) {
            workout.currentParticipants++
            if (workout.currentParticipants >= workout.maxParticipants) {
                workout.isAvailable = false
            }

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            bookings.add(
                Booking(
                    workoutId = workoutId,
                    clientId = clientId,
                    bookingDate = today
                )
            )
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
        }
    }

    fun cancelWorkout(workoutId: String) {
        val workout = workouts.find { it.id == workoutId }
        workout?.let {
            // Отменяем все бронирования на эту тренировку
            bookings.filter { b -> b.workoutId == workoutId && b.status == "confirmed" }
                .forEach { b -> b.status = "cancelled" }
            // Удаляем тренировку
            workouts.remove(it)
        }
    }
}

// ==================== ГЛАВНАЯ АКТИВНОСТЬ ====================
class MainActivity : AppCompatActivity() {

    private lateinit var currentUser: User
    private var isLoggedIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showLoginScreen()
    }

    // ==================== ЭКРАН АВТОРИЗАЦИИ ====================
    private fun showLoginScreen() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48)

            // Заголовок
            val title = TextView(this@MainActivity).apply {
                text = "🏋️‍♂️ Фитнес-Клуб"
                textSize = 28f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setTextColor(resources.getColor(android.R.color.holo_blue_dark))
                setPadding(0, 0, 0, 48)
            }
            addView(title)

            // Поле для логина
            val etUsername = EditText(this@MainActivity).apply {
                hint = "Логин"
                setSingleLine(true)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 16
                }
            }
            addView(etUsername)

            // Поле для пароля
            val etPassword = EditText(this@MainActivity).apply {
                hint = "Пароль"
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                setSingleLine(true)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 24
                }
            }
            addView(etPassword)

            // Кнопка входа
            val btnLogin = Button(this@MainActivity).apply {
                text = "Войти"
                setBackgroundColor(resources.getColor(android.R.color.holo_blue_dark))
                setTextColor(resources.getColor(android.R.color.white))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 16
                }
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

            // Кнопка регистрации
            val btnRegister = Button(this@MainActivity).apply {
                text = "Зарегистрироваться"
                setBackgroundColor(resources.getColor(android.R.color.darker_gray))
                setTextColor(resources.getColor(android.R.color.white))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    showRegistrationDialog()
                }
            }
            addView(btnRegister)

            // Тестовые учетные данные
            val credentials = TextView(this@MainActivity).apply {
                text = "Тестовые данные:\nАдмин: admin / admin123\nТренер: trainer1 / trainer123\nКлиент: client1 / client123"
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(0, 32, 0, 0)
                setTextColor(resources.getColor(android.R.color.darker_gray))
            }
            addView(credentials)
        }

        setContentView(layout)
    }

    // ==================== РЕГИСТРАЦИЯ ====================
    private fun showRegistrationDialog() {
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24)

            val etName = EditText(this@MainActivity).apply {
                hint = "Полное имя *"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
            }
            addView(etName)

            val etUsername = EditText(this@MainActivity).apply {
                hint = "Логин *"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
            }
            addView(etUsername)

            val etPassword = EditText(this@MainActivity).apply {
                hint = "Пароль *"
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
            }
            addView(etPassword)

            val etEmail = EditText(this@MainActivity).apply {
                hint = "Email"
                inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
            }
            addView(etEmail)

            val etPhone = EditText(this@MainActivity).apply {
                hint = "Телефон"
                inputType = android.text.InputType.TYPE_CLASS_PHONE
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 16
                }
            }
            addView(etPhone)

            // Выбор роли
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
                val name = (dialogLayout.getChildAt(0) as EditText).text.toString()
                val username = (dialogLayout.getChildAt(1) as EditText).text.toString()
                val password = (dialogLayout.getChildAt(2) as EditText).text.toString()
                val email = (dialogLayout.getChildAt(3) as EditText).text.toString()
                val phone = (dialogLayout.getChildAt(4) as EditText).text.toString()
                val role = (dialogLayout.getChildAt(5) as Spinner).selectedItem.toString()

                if (name.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
                    // Проверяем, не занят ли логин
                    val isUsernameTaken = FitnessDatabase.users.any { it.username == username }

                    if (isUsernameTaken) {
                        Toast.makeText(this, "Этот логин уже занят! Выберите другой.", Toast.LENGTH_LONG).show()
                    } else {
                        val newUser = User(
                            username = username,
                            password = password,
                            role = if (role == "Тренер") "trainer" else "client",
                            name = name,
                            email = email,
                            phone = phone
                        )

                        FitnessDatabase.users.add(newUser)

                        // Если это тренер, создаем профиль тренера
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

                        // Автоматически входим под новым пользователем
                        currentUser = newUser
                        isLoggedIn = true

                        Toast.makeText(this, "Регистрация успешна! Добро пожаловать, $name!", Toast.LENGTH_SHORT).show()
                        showDashboard()
                    }
                } else {
                    Toast.makeText(this, "Заполните обязательные поля (имя, логин и пароль)", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    // ==================== ГЛАВНЫЙ ЭКРАН ====================
    private fun showDashboard() {
        when (currentUser.role) {
            "admin" -> showAdminDashboard()
            "trainer" -> showTrainerDashboard()
            "client" -> showClientDashboard()
        }
    }

    // ==================== ПАНЕЛЬ АДМИНИСТРАТОРА ====================
    private fun showAdminDashboard() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24)

            // Заголовок
            val title = TextView(this@MainActivity).apply {
                text = "👑 Панель администратора"
                textSize = 22f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 24)
            }
            addView(title)

            // Приветствие
            val welcome = TextView(this@MainActivity).apply {
                text = "Добро пожаловать, ${currentUser.name}!"
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 32)
            }
            addView(welcome)

            // Кнопка добавления тренера
            val btnAddTrainer = Button(this@MainActivity).apply {
                text = "➕ Добавить тренера"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
                setOnClickListener {
                    showAddTrainerDialog()
                }
            }
            addView(btnAddTrainer)

            // Кнопка просмотра тренеров
            val btnViewTrainers = Button(this@MainActivity).apply {
                text = "👥 Все тренеры"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
                setOnClickListener {
                    showAllTrainers()
                }
            }
            addView(btnViewTrainers)

            // Кнопка просмотра статистики
            val btnStatistics = Button(this@MainActivity).apply {
                text = "📊 Статистика клуба"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
                setOnClickListener {
                    showStatistics()
                }
            }
            addView(btnStatistics)

            // Кнопка просмотра всех тренировок
            val btnAllWorkouts = Button(this@MainActivity).apply {
                text = "📅 Все тренировки"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
                setOnClickListener {
                    showAllWorkouts(true)
                }
            }
            addView(btnAllWorkouts)

            // Кнопка управления пользователями
            val btnManageUsers = Button(this@MainActivity).apply {
                text = "👤 Управление пользователями"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
                setOnClickListener {
                    showAllUsers()
                }
            }
            addView(btnManageUsers)

            // Кнопка выхода
            val btnLogout = Button(this@MainActivity).apply {
                text = "🚪 Выйти"
                setBackgroundColor(resources.getColor(android.R.color.holo_red_light))
                setTextColor(resources.getColor(android.R.color.white))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 32
                }
                setOnClickListener {
                    isLoggedIn = false
                    showLoginScreen()
                }
            }
            addView(btnLogout)
        }

        setContentView(layout)
    }

    private fun showAddTrainerDialog() {
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24)

            val etName = EditText(this@MainActivity).apply {
                hint = "Имя тренера"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
            }
            addView(etName)

            val etSpecialization = EditText(this@MainActivity).apply {
                hint = "Специализация"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
            }
            addView(etSpecialization)

            val etExperience = EditText(this@MainActivity).apply {
                hint = "Опыт работы (лет)"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
            }
            addView(etExperience)

            val etDescription = EditText(this@MainActivity).apply {
                hint = "Описание"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
            }
            addView(etDescription)

            val etEmail = EditText(this@MainActivity).apply {
                hint = "Email"
                inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
            }
            addView(etEmail)

            val etPhone = EditText(this@MainActivity).apply {
                hint = "Телефон"
                inputType = android.text.InputType.TYPE_CLASS_PHONE
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
                    // Создаем пользователя для тренера
                    val username = name.lowercase(Locale.getDefault()).replace(" ", "") + "_trainer"
                    val password = "temp123"

                    val trainerUser = User(
                        username = username,
                        password = password,
                        role = "trainer",
                        name = name,
                        email = email,
                        phone = phone
                    )

                    FitnessDatabase.users.add(trainerUser)

                    // Создаем профиль тренера
                    val trainer = Trainer(
                        userId = trainerUser.id,
                        name = name,
                        specialization = if (specialization.isNotEmpty()) specialization else "Фитнес",
                        experience = experience,
                        description = description
                    )

                    FitnessDatabase.trainers.add(trainer)

                    Toast.makeText(
                        this,
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
            setPadding(16)

            val scrollView = ScrollView(this@MainActivity)
            val container = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
            }

            // Заголовок
            val title = TextView(this@MainActivity).apply {
                text = "👥 Все тренеры"
                textSize = 20f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 16)
            }
            container.addView(title)

            // Список тренеров
            FitnessDatabase.trainers.forEach { trainer ->
                val card = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16)
                    background = resources.getDrawable(android.R.drawable.dialog_holo_light_frame)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 12
                    }

                    val name = TextView(this@MainActivity).apply {
                        text = trainer.name
                        textSize = 18f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setPadding(0, 0, 0, 8)
                    }
                    addView(name)

                    val specialization = TextView(this@MainActivity).apply {
                        text = "Специализация: ${trainer.specialization}"
                        setPadding(0, 0, 0, 4)
                    }
                    addView(specialization)

                    val experience = TextView(this@MainActivity).apply {
                        text = "Опыт: ${trainer.experience} лет"
                        setPadding(0, 0, 0, 4)
                    }
                    addView(experience)

                    val rating = TextView(this@MainActivity).apply {
                        text = "Рейтинг: ${trainer.rating}/5.0"
                        setPadding(0, 0, 0, 8)
                    }
                    addView(rating)

                    val description = TextView(this@MainActivity).apply {
                        text = trainer.description
                        textSize = 14f
                        setTextColor(resources.getColor(android.R.color.darker_gray))
                    }
                    addView(description)

                    setOnClickListener {
                        showTrainerDetails(trainer, true)
                    }
                }
                container.addView(card)
            }

            // Кнопка назад
            val btnBack = Button(this@MainActivity).apply {
                text = "Назад"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16
                }
                setOnClickListener {
                    showAdminDashboard()
                }
            }
            container.addView(btnBack)

            scrollView.addView(container)
            addView(scrollView)
        }

        setContentView(layout)
    }

    private fun showStatistics() {
        val totalTrainers = FitnessDatabase.trainers.size
        val totalWorkouts = FitnessDatabase.workouts.size
        val totalClients = FitnessDatabase.users.count { it.role == "client" }
        val totalBookings = FitnessDatabase.bookings.count { it.status == "confirmed" }

        val availableWorkouts = FitnessDatabase.workouts.count { it.isAvailable }
        val avgParticipants = if (FitnessDatabase.workouts.isNotEmpty()) {
            FitnessDatabase.workouts.map { it.currentParticipants.toDouble() / it.maxParticipants }.average() * 100
        } else 0.0

        val message = """
            📊 Статистика фитнес-клуба
            
            👥 Персонал:
            • Тренеров: $totalTrainers
            • Клиентов: $totalClients
            
            📅 Тренировки:
            • Всего тренировок: $totalWorkouts
            • Доступных тренировок: $availableWorkouts
            • Активных бронирований: $totalBookings
            
            📈 Эффективность:
            • Средняя заполняемость: ${String.format("%.1f", avgParticipants)}%
            
            💰 Финансы:
            • Общий доход: ${FitnessDatabase.workouts.sumOf { it.price * it.currentParticipants }} руб.
            
            🎯 Цели:
            • Привлечь еще ${15 - totalClients} клиентов
            • Добавить ${5 - totalTrainers} тренеров
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Статистика клуба")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAllWorkouts(isAdmin: Boolean = false) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16)

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

            val workouts = if (isAdmin) FitnessDatabase.workouts else FitnessDatabase.getAvailableWorkouts()

            if (workouts.isEmpty()) {
                val emptyText = TextView(this@MainActivity).apply {
                    text = "Нет доступных тренировок"
                    gravity = Gravity.CENTER
                    setPadding(0, 32, 0, 32)
                }
                container.addView(emptyText)
            } else {
                workouts.forEach { workout ->
                    val trainer = FitnessDatabase.trainers.find { it.id == workout.trainerId }

                    val card = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(16)
                        background = resources.getDrawable(android.R.drawable.dialog_holo_light_frame)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = 12
                        }

                        val titleText = TextView(this@MainActivity).apply {
                            text = workout.title
                            textSize = 18f
                            setTypeface(typeface, android.graphics.Typeface.BOLD)
                            setPadding(0, 0, 0, 8)
                        }
                        addView(titleText)

                        val trainerText = TextView(this@MainActivity).apply {
                            text = "Тренер: ${trainer?.name ?: "Неизвестно"}"
                            setPadding(0, 0, 0, 4)
                        }
                        addView(trainerText)

                        val dateTime = TextView(this@MainActivity).apply {
                            text = "📅 ${workout.date} 🕒 ${workout.time} (${workout.duration} мин.)"
                            setPadding(0, 0, 0, 4)
                        }
                        addView(dateTime)

                        val location = TextView(this@MainActivity).apply {
                            text = "📍 ${workout.location}"
                            setPadding(0, 0, 0, 4)
                        }
                        addView(location)

                        val participants = TextView(this@MainActivity).apply {
                            text = "👥 ${workout.currentParticipants}/${workout.maxParticipants} участников"
                            setPadding(0, 0, 0, 8)
                        }
                        addView(participants)

                        val price = TextView(this@MainActivity).apply {
                            text = "💰 ${workout.price} руб."
                            setTypeface(typeface, android.graphics.Typeface.BOLD)
                            setTextColor(resources.getColor(android.R.color.holo_green_dark))
                        }
                        addView(price)

                        setOnClickListener {
                            showWorkoutDetails(workout, isAdmin)
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
                ).apply {
                    topMargin = 16
                }
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

    private fun showAllUsers() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16)

            val scrollView = ScrollView(this@MainActivity)
            val container = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
            }

            val title = TextView(this@MainActivity).apply {
                text = "👤 Все пользователи"
                textSize = 20f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 16)
            }
            container.addView(title)

            FitnessDatabase.users.forEach { user ->
                val roleText = when(user.role) {
                    "admin" -> "👑 Администратор"
                    "trainer" -> "🏋️ Тренер"
                    else -> "👤 Клиент"
                }

                val card = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16)
                    background = resources.getDrawable(android.R.drawable.dialog_holo_light_frame)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 12
                    }

                    val name = TextView(this@MainActivity).apply {
                        text = "${user.name} ($roleText)"
                        textSize = 16f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setPadding(0, 0, 0, 4)
                    }
                    addView(name)

                    val contact = TextView(this@MainActivity).apply {
                        text = "📧 ${user.email}\n📱 ${user.phone}"
                        setPadding(0, 0, 0, 4)
                    }
                    addView(contact)

                    val loginInfo = TextView(this@MainActivity).apply {
                        text = "Логин: ${user.username}"
                        textSize = 12f
                        setTextColor(resources.getColor(android.R.color.darker_gray))
                    }
                    addView(loginInfo)
                }
                container.addView(card)
            }

            val btnBack = Button(this@MainActivity).apply {
                text = "Назад"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16
                }
                setOnClickListener {
                    showAdminDashboard()
                }
            }
            container.addView(btnBack)

            scrollView.addView(container)
            addView(scrollView)
        }

        setContentView(layout)
    }

    // ==================== ПАНЕЛЬ ТРЕНЕРА ====================
    private fun showTrainerDashboard() {
        val trainer = FitnessDatabase.getTrainerByUserId(currentUser.id)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24)

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
                ).apply {
                    bottomMargin = 12
                }
                setOnClickListener {
                    showCreateWorkoutDialog()
                }
            }
            addView(btnCreateWorkout)

            val btnMyWorkouts = Button(this@MainActivity).apply {
                text = "📅 Мои тренировки"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
                setOnClickListener {
                    showMyWorkouts()
                }
            }
            addView(btnMyWorkouts)

            val btnMySchedule = Button(this@MainActivity).apply {
                text = "🗓️ Мое расписание"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
                setOnClickListener {
                    showTrainerSchedule()
                }
            }
            addView(btnMySchedule)

            val btnMyClients = Button(this@MainActivity).apply {
                text = "👥 Мои клиенты"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
                setOnClickListener {
                    showMyClients()
                }
            }
            addView(btnMyClients)

            val btnProfile = Button(this@MainActivity).apply {
                text = "👤 Мой профиль"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
                setOnClickListener {
                    showTrainerProfile()
                }
            }
            addView(btnProfile)

            val btnLogout = Button(this@MainActivity).apply {
                text = "🚪 Выйти"
                setBackgroundColor(resources.getColor(android.R.color.holo_red_light))
                setTextColor(resources.getColor(android.R.color.white))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 32
                }
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
            setPadding(24)

            val etTitle = EditText(this@MainActivity).apply {
                hint = "Название тренировки"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
            }
            addView(etTitle)

            val etDescription = EditText(this@MainActivity).apply {
                hint = "Описание тренировки"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
            }
            addView(etDescription)

            val etDate = EditText(this@MainActivity).apply {
                hint = "Дата (ГГГГ-ММ-ДД)"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
            }
            addView(etDate)

            val etTime = EditText(this@MainActivity).apply {
                hint = "Время (ЧЧ:ММ)"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
            }
            addView(etTime)

            val etDuration = EditText(this@MainActivity).apply {
                hint = "Длительность (минуты)"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
            }
            addView(etDuration)

            val etMaxParticipants = EditText(this@MainActivity).apply {
                hint = "Максимум участников"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
            }
            addView(etMaxParticipants)

            val etLocation = EditText(this@MainActivity).apply {
                hint = "Место проведения"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
            }
            addView(etLocation)

            val etPrice = EditText(this@MainActivity).apply {
                hint = "Цена (руб.)"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            addView(etPrice)
        }

        AlertDialog.Builder(this)
            .setTitle("Создать новую тренировку")
            .setView(dialogLayout)
            .setPositiveButton("Создать") { dialog, which ->
                val title = (dialogLayout.getChildAt(0) as EditText).text.toString()
                val description = (dialogLayout.getChildAt(1) as EditText).text.toString()
                val date = (dialogLayout.getChildAt(2) as EditText).text.toString()
                val time = (dialogLayout.getChildAt(3) as EditText).text.toString()
                val duration = (dialogLayout.getChildAt(4) as EditText).text.toString().toIntOrNull() ?: 60
                val maxParticipants = (dialogLayout.getChildAt(5) as EditText).text.toString().toIntOrNull() ?: 10
                val location = (dialogLayout.getChildAt(6) as EditText).text.toString()
                val price = (dialogLayout.getChildAt(7) as EditText).text.toString().toIntOrNull() ?: 500

                if (title.isNotEmpty() && date.isNotEmpty() && time.isNotEmpty()) {
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
                    Toast.makeText(this, "Тренировка создана!", Toast.LENGTH_SHORT).show()
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
            setPadding(16)

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
                myWorkouts.forEach { workout ->
                    val card = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(16)
                        background = resources.getDrawable(android.R.drawable.dialog_holo_light_frame)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = 12
                        }

                        val titleText = TextView(this@MainActivity).apply {
                            text = workout.title
                            textSize = 18f
                            setTypeface(typeface, android.graphics.Typeface.BOLD)
                            setPadding(0, 0, 0, 8)
                        }
                        addView(titleText)

                        val dateTime = TextView(this@MainActivity).apply {
                            text = "📅 ${workout.date} 🕒 ${workout.time}"
                            setPadding(0, 0, 0, 4)
                        }
                        addView(dateTime)

                        val participants = TextView(this@MainActivity).apply {
                            text = "👥 ${workout.currentParticipants}/${workout.maxParticipants} участников"
                            setPadding(0, 0, 0, 4)
                        }
                        addView(participants)

                        val status = TextView(this@MainActivity).apply {
                            text = if (workout.isAvailable) "✅ Доступна" else "❌ Заполнена"
                            setTextColor(
                                if (workout.isAvailable) resources.getColor(android.R.color.holo_green_dark)
                                else resources.getColor(android.R.color.holo_red_dark)
                            )
                        }
                        addView(status)

                        setOnClickListener {
                            showWorkoutDetails(workout, true)
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
                ).apply {
                    topMargin = 16
                }
                setOnClickListener {
                    showTrainerDashboard()
                }
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
        val upcomingWorkouts = myWorkouts.filter { it.date >= today }.sortedBy { it.date }

        val message = if (upcomingWorkouts.isEmpty()) {
            "У вас нет запланированных тренировок"
        } else {
            val builder = StringBuilder()
            builder.append("📅 Расписание тренировок\n\n")

            upcomingWorkouts.forEach { workout ->
                builder.append("🏋️ ${workout.title}\n")
                builder.append("   📅 ${workout.date} 🕒 ${workout.time}\n")
                builder.append("   ⏱️ ${workout.duration} мин.\n")
                builder.append("   👥 ${workout.currentParticipants}/${workout.maxParticipants}\n")
                builder.append("   📍 ${workout.location}\n")
                builder.append("   💰 ${workout.price} руб.\n\n")
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

        // Собираем всех клиентов, записанных на тренировки тренера
        val clientIds = mutableSetOf<String>()
        myWorkouts.forEach { workout ->
            FitnessDatabase.bookings.filter { it.workoutId == workout.id && it.status == "confirmed" }
                .forEach { clientIds.add(it.clientId) }
        }

        val clients = FitnessDatabase.users.filter { it.id in clientIds }

        val message = if (clients.isEmpty()) {
            "У вас пока нет клиентов"
        } else {
            val builder = StringBuilder()
            builder.append("👥 Мои клиенты\n\n")

            clients.forEach { client ->
                val clientWorkouts = myWorkouts.filter { workout ->
                    FitnessDatabase.bookings.any {
                        it.workoutId == workout.id && it.clientId == client.id && it.status == "confirmed"
                    }
                }

                builder.append("👤 ${client.name}\n")
                builder.append("   📧 ${client.email}\n")
                builder.append("   📱 ${client.phone}\n")
                builder.append("   📅 Записан на тренировок: ${clientWorkouts.size}\n\n")
            }

            builder.toString()
        }

        AlertDialog.Builder(this)
            .setTitle("Мои клиенты")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showTrainerProfile() {
        val trainer = FitnessDatabase.getTrainerByUserId(currentUser.id)

        val message = """
            👤 Мой профиль тренера
            
            Имя: ${trainer?.name ?: currentUser.name}
            Специализация: ${trainer?.specialization ?: "Не указана"}
            Опыт: ${trainer?.experience ?: 0} лет
            Рейтинг: ${trainer?.rating ?: 0.0}/5.0
            
            ${trainer?.description ?: ""}
            
            Контакты:
            📧 ${currentUser.email}
            📱 ${currentUser.phone}
            
            Статистика:
            • Создано тренировок: ${FitnessDatabase.getWorkoutsByTrainer(trainer?.id ?: "").size}
            • Активных клиентов: ${FitnessDatabase.bookings.count { booking ->
            val workout = FitnessDatabase.workouts.find { it.id == booking.workoutId }
            workout?.trainerId == trainer?.id && booking.status == "confirmed"
        }}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Профиль тренера")
            .setMessage(message)
            .setPositiveButton("Редактировать профиль") { dialog, which ->
                Toast.makeText(this, "Функция редактирования в разработке", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("OK", null)
            .show()
    }

    // ==================== ПАНЕЛЬ КЛИЕНТА ====================
    private fun showClientDashboard() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24)

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
                setPadding(0, 0, 0, 32)
            }
            addView(welcome)

            val btnAvailableWorkouts = Button(this@MainActivity).apply {
                text = "🏋️ Доступные тренировки"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
                setOnClickListener {
                    showAllWorkouts(false)
                }
            }
            addView(btnAvailableWorkouts)

            val btnViewTrainers = Button(this@MainActivity).apply {
                text = "👥 Наши тренеры"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
                setOnClickListener {
                    showClientTrainers()
                }
            }
            addView(btnViewTrainers)

            val btnMyBookings = Button(this@MainActivity).apply {
                text = "📖 Мои записи"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
                setOnClickListener {
                    showMyBookings()
                }
            }
            addView(btnMyBookings)

            val btnProfile = Button(this@MainActivity).apply {
                text = "👤 Мой профиль"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
                setOnClickListener {
                    showClientProfile()
                }
            }
            addView(btnProfile)

            val btnLogout = Button(this@MainActivity).apply {
                text = "🚪 Выйти"
                setBackgroundColor(resources.getColor(android.R.color.holo_red_light))
                setTextColor(resources.getColor(android.R.color.white))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 32
                }
                setOnClickListener {
                    isLoggedIn = false
                    showLoginScreen()
                }
            }
            addView(btnLogout)
        }

        setContentView(layout)
    }

    private fun showClientTrainers() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16)

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
                val card = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16)
                    background = resources.getDrawable(android.R.drawable.dialog_holo_light_frame)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 12
                    }

                    val name = TextView(this@MainActivity).apply {
                        text = trainer.name
                        textSize = 18f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setPadding(0, 0, 0, 8)
                    }
                    addView(name)

                    val specialization = TextView(this@MainActivity).apply {
                        text = "🏆 ${trainer.specialization}"
                        setPadding(0, 0, 0, 4)
                    }
                    addView(specialization)

                    val experience = TextView(this@MainActivity).apply {
                        text = "⏱️ Опыт: ${trainer.experience} лет"
                        setPadding(0, 0, 0, 4)
                    }
                    addView(experience)

                    val rating = TextView(this@MainActivity).apply {
                        text = "⭐ Рейтинг: ${trainer.rating}/5.0"
                        setPadding(0, 0, 0, 8)
                    }
                    addView(rating)

                    val description = TextView(this@MainActivity).apply {
                        text = trainer.description
                        textSize = 14f
                        setTextColor(resources.getColor(android.R.color.darker_gray))
                    }
                    addView(description)

                    setOnClickListener {
                        showTrainerDetails(trainer, false)
                    }
                }
                container.addView(card)
            }

            val btnBack = Button(this@MainActivity).apply {
                text = "Назад"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16
                }
                setOnClickListener {
                    showClientDashboard()
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

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16)

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

                    if (workout != null) {
                        val card = LinearLayout(this@MainActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(16)
                            background = resources.getDrawable(android.R.drawable.dialog_holo_light_frame)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                bottomMargin = 12
                            }

                            val titleText = TextView(this@MainActivity).apply {
                                text = workout.title
                                textSize = 18f
                                setTypeface(typeface, android.graphics.Typeface.BOLD)
                                setPadding(0, 0, 0, 8)
                            }
                            addView(titleText)

                            val trainerText = TextView(this@MainActivity).apply {
                                text = "Тренер: ${trainer?.name ?: "Неизвестно"}"
                                setPadding(0, 0, 0, 4)
                            }
                            addView(trainerText)

                            val dateTime = TextView(this@MainActivity).apply {
                                text = "Дата: ${workout.date} ${workout.time}"
                                setPadding(0, 0, 0, 4)
                            }
                            addView(dateTime)

                            val location = TextView(this@MainActivity).apply {
                                text = "Место: ${workout.location}"
                                setPadding(0, 0, 0, 4)
                            }
                            addView(location)

                            val status = TextView(this@MainActivity).apply {
                                text = "✅ Подтверждено"
                                setTextColor(resources.getColor(android.R.color.holo_green_dark))
                                setPadding(0, 8, 0, 0)
                            }
                            addView(status)

                            setOnClickListener {
                                AlertDialog.Builder(this@MainActivity)
                                    .setTitle("Детали записи")
                                    .setMessage("""
                                        Тренировка: ${workout.title}
                                        Тренер: ${trainer?.name}
                                        Дата и время: ${workout.date} ${workout.time}
                                        Длительность: ${workout.duration} мин.
                                        Место: ${workout.location}
                                        Цена: ${workout.price} руб.
                                        Статус: Подтверждено
                                    """.trimIndent())
                                    .setPositiveButton("OK", null)
                                    .setNeutralButton("Отменить запись") { dialog, which ->
                                        FitnessDatabase.cancelBooking(booking.id)
                                        Toast.makeText(this@MainActivity, "Запись отменена", Toast.LENGTH_SHORT).show()
                                        showMyBookings()
                                    }
                                    .show()
                            }
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
                ).apply {
                    topMargin = 16
                }
                setOnClickListener {
                    showClientDashboard()
                }
            }
            container.addView(btnBack)

            scrollView.addView(container)
            addView(scrollView)
        }

        setContentView(layout)
    }

    private fun showClientProfile() {
        val myBookings = FitnessDatabase.getBookingsByClient(currentUser.id)

        val message = """
            👤 Мой профиль
            
            Имя: ${currentUser.name}
            Email: ${currentUser.email}
            Телефон: ${currentUser.phone}
            
            📊 Моя статистика:
            • Активных записей: ${myBookings.size}
            • Всего тренировок: ${FitnessDatabase.bookings.count { it.clientId == currentUser.id }}
            • Потрачено: ${myBookings.sumOf { booking ->
            val workout = FitnessDatabase.workouts.find { it.id == booking.workoutId }
            workout?.price ?: 0
        }} руб.
            
            🎯 Мои цели:
            • Посетить еще ${5 - myBookings.size} тренировок
            • Попробовать разные направления
            
            💪 Рекомендации:
            ${if (myBookings.size < 3) "Начните с 2-3 тренировок в неделю"
        else "Продолжайте в том же темпе!"}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Мой профиль")
            .setMessage(message)
            .setPositiveButton("Редактировать профиль") { dialog, which ->
                Toast.makeText(this, "Функция редактирования в разработке", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("OK", null)
            .show()
    }

    // ==================== ОБЩИЕ МЕТОДЫ ====================
    private fun showTrainerDetails(trainer: Trainer, isAdmin: Boolean) {
        val trainerWorkouts = FitnessDatabase.workouts.filter { it.trainerId == trainer.id }
        val availableWorkouts = trainerWorkouts.filter { it.isAvailable && it.currentParticipants < it.maxParticipants }

        val message = """
            ${trainer.name}
            
            🏆 Специализация:
            ${trainer.specialization}
            
            ⏱️ Опыт работы:
            ${trainer.experience} лет
            
            ⭐ Рейтинг:
            ${trainer.rating}/5.0
            
            📝 Описание:
            ${trainer.description}
            
            📅 Доступные тренировки:
            ${if (availableWorkouts.isEmpty()) "Нет доступных тренировок"
        else availableWorkouts.joinToString("\n") { "• ${it.title} (${it.date} ${it.time})" }}
            
            👥 Клиентов записано:
            ${trainerWorkouts.sumOf { it.currentParticipants }}
        """.trimIndent()

        val builder = AlertDialog.Builder(this)
            .setTitle("👤 ${trainer.name}")
            .setMessage(message)
            .setPositiveButton("OK", null)

        if (isAdmin) {
            builder.setNeutralButton("Удалить тренера") { dialog, which ->
                // Удаляем пользователя тренера
                FitnessDatabase.users.removeIf { it.id == trainer.userId }
                // Удаляем профиль тренера
                FitnessDatabase.trainers.remove(trainer)
                // Удаляем тренировки тренера
                FitnessDatabase.workouts.removeAll { it.trainerId == trainer.id }
                Toast.makeText(this, "Тренер удален", Toast.LENGTH_SHORT).show()
                showAllTrainers()
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

        val message = """
            ${workout.title}
            
            🏋️ Тренер:
            ${trainer?.name ?: "Неизвестно"}
            
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
            
            ${if (!workout.isAvailable) "❌ Тренировка заполнена"
        else if (alreadyBooked) "✅ Вы уже записаны"
        else "✅ Доступна для записи"}
        """.trimIndent()

        val builder = AlertDialog.Builder(this)
            .setTitle("🏋️ ${workout.title}")
            .setMessage(message)

        if (isClient && workout.isAvailable && !alreadyBooked) {
            builder.setPositiveButton("Записаться") { dialog, which ->
                val success = FitnessDatabase.bookWorkout(workout.id, currentUser.id)
                if (success) {
                    Toast.makeText(this, "Вы успешно записались на тренировку!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Не удалось записаться на тренировку", Toast.LENGTH_SHORT).show()
                }
            }
            builder.setNeutralButton("Подробнее о тренере") { dialog, which ->
                trainer?.let { showTrainerDetails(it, false) }
            }
            builder.setNegativeButton("Отмена", null)
        } else if (isTrainerOrAdmin && currentUser.role == "trainer" && workout.trainerId == trainer?.id) {
            builder.setPositiveButton("OK", null)
            builder.setNeutralButton("Отменить тренировку") { dialog, which ->
                AlertDialog.Builder(this)
                    .setTitle("Подтверждение")
                    .setMessage("Вы уверены, что хотите отменить эту тренировку? Все клиенты будут уведомлены.")
                    .setPositiveButton("Да, отменить") { d, w ->
                        FitnessDatabase.cancelWorkout(workout.id)
                        Toast.makeText(this, "Тренировка отменена", Toast.LENGTH_SHORT).show()
                        showMyWorkouts()
                    }
                    .setNegativeButton("Нет", null)
                    .show()
            }
        } else if (isTrainerOrAdmin && currentUser.role == "admin") {
            builder.setPositiveButton("OK", null)
            builder.setNeutralButton("Удалить тренировку") { dialog, which ->
                FitnessDatabase.workouts.removeIf { it.id == workout.id }
                Toast.makeText(this, "Тренировка удалена", Toast.LENGTH_SHORT).show()
                showAllWorkouts(true)
            }
        } else {
            builder.setPositiveButton("OK", null)
            if (isClient && alreadyBooked) {
                builder.setNeutralButton("Отменить запись") { dialog, which ->
                    val booking = FitnessDatabase.bookings.find {
                        it.workoutId == workout.id && it.clientId == currentUser.id && it.status == "confirmed"
                    }
                    booking?.let {
                        FitnessDatabase.cancelBooking(it.id)
                        Toast.makeText(this, "Запись отменена", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        builder.show()
    }
}