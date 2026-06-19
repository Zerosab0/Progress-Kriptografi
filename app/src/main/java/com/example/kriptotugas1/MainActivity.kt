package com.example.kriptotugas1

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow

class MainActivity : AppCompatActivity() {
    private lateinit var authGate: View
    private lateinit var pageContainer: View
    private lateinit var pageHome: View
    private lateinit var pageVault: View
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var btnSaveVault: MaterialButton
    private lateinit var rvVault: RecyclerView
    private lateinit var vaultAdapter: VaultAdapter

    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var passwordInput: TextInputEditText
    private lateinit var progressStrength: LinearProgressIndicator
    private lateinit var tvStrengthLabel: TextView
    private lateinit var tvStrengthScore: TextView
    private lateinit var tvFeedback: TextView
    private lateinit var tvAnalyzedAt: TextView
    private lateinit var tvLengthValue: TextView
    private lateinit var tvCompositionValue: TextView
    private lateinit var tvEntropyValue: TextView
    private lateinit var tvGuessTimeValue: TextView
    private lateinit var tvHashMd5Value: TextView
    private lateinit var tvHashValue: TextView
    private lateinit var tvHashSha512Value: TextView
    private lateinit var tvRuleLength: TextView
    private lateinit var tvRuleUppercase: TextView
    private lateinit var tvRuleLowercase: TextView
    private lateinit var tvRuleDigit: TextView
    private lateinit var tvRuleSymbol: TextView
    private lateinit var tvRulePattern: TextView

    private lateinit var authEmailInput: TextInputEditText
    private lateinit var authPasswordInput: TextInputEditText
    private lateinit var tvAuthStatus: TextView

    private var firebaseAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private var firebaseReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bindViews()
        initializeFirebaseServices()
        configureActions()
        resetDashboard()
        updateAuthGate()
    }

    override fun onStart() {
        super.onStart()
        if (::authGate.isInitialized) {
            updateAuthStatus()
            updateAuthGate()
        }
    }

    private fun bindViews() {
        authGate = findViewById(R.id.authGate)
        pageContainer = findViewById(R.id.pageContainer)
        pageHome = findViewById(R.id.pageHome)
        pageVault = findViewById(R.id.pageVault)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        btnSaveVault = findViewById(R.id.btnSaveVault)
        rvVault = findViewById(R.id.rvVault)

        passwordInputLayout = findViewById(R.id.passwordInputLayout)
        passwordInput = findViewById(R.id.etPassword)
        progressStrength = findViewById(R.id.progressStrength)
        tvStrengthLabel = findViewById(R.id.tvStrengthLabel)
        tvStrengthScore = findViewById(R.id.tvStrengthScore)
        tvFeedback = findViewById(R.id.tvFeedback)
        tvAnalyzedAt = findViewById(R.id.tvAnalyzedAt)
        tvLengthValue = findViewById(R.id.tvLengthValue)
        tvCompositionValue = findViewById(R.id.tvCompositionValue)
        tvEntropyValue = findViewById(R.id.tvEntropyValue)
        tvGuessTimeValue = findViewById(R.id.tvGuessTimeValue)
        tvHashMd5Value = findViewById(R.id.tvHashMd5Value)
        tvHashValue = findViewById(R.id.tvHashValue)
        tvHashSha512Value = findViewById(R.id.tvHashSha512Value)
        tvRuleLength = findViewById(R.id.tvRuleLength)
        tvRuleUppercase = findViewById(R.id.tvRuleUppercase)
        tvRuleLowercase = findViewById(R.id.tvRuleLowercase)
        tvRuleDigit = findViewById(R.id.tvRuleDigit)
        tvRuleSymbol = findViewById(R.id.tvRuleSymbol)
        tvRulePattern = findViewById(R.id.tvRulePattern)

        authEmailInput = findViewById(R.id.etAuthEmail)
        authPasswordInput = findViewById(R.id.etAuthPassword)
        tvAuthStatus = findViewById(R.id.tvAuthStatus)
    }

    private fun initializeFirebaseServices() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            firebaseReady = FirebaseApp.getApps(this).isNotEmpty()
            if (firebaseReady) {
                firebaseAuth = FirebaseAuth.getInstance()
                firestore = FirebaseFirestore.getInstance()
            }
        } catch (exception: Exception) {
            firebaseReady = false
            firebaseAuth = null
        }
        updateAuthStatus()
    }

    private fun configureActions() {
        findViewById<MaterialButton>(R.id.btnAuthLogin).setOnClickListener {
            loginFirebaseUser()
        }
        findViewById<MaterialButton>(R.id.btnAuthRegister).setOnClickListener {
            registerFirebaseUser()
        }
        findViewById<MaterialButton>(R.id.btnAuthLogout).setOnClickListener {
            signOutFirebaseUser()
        }

        findViewById<MaterialButton>(R.id.btnAnalyze).setOnClickListener {
            analyzePassword()
        }

        findViewById<MaterialButton>(R.id.btnClear).setOnClickListener {
            passwordInput.text?.clear()
            passwordInputLayout.error = null
            resetDashboard()
        }

        findViewById<MaterialButton>(R.id.btnGenerate).setOnClickListener {
            val generated = generateStrongPassword()
            passwordInput.setText(generated)
            passwordInputLayout.error = null
            analyzePassword()
        }

        findViewById<MaterialButton>(R.id.btnCopy).setOnClickListener {
            val password = passwordInput.textValue()
            if (password.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Password", password)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Password disalin ke clipboard", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Tidak ada password untuk disalin", Toast.LENGTH_SHORT).show()
            }
        }

        btnSaveVault.setOnClickListener {
            showSaveVaultDialog()
        }

        vaultAdapter = VaultAdapter(emptyList())
        rvVault.layoutManager = LinearLayoutManager(this)
        rvVault.adapter = vaultAdapter

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    pageHome.visibility = View.VISIBLE
                    pageVault.visibility = View.GONE
                    true
                }
                R.id.nav_vault -> {
                    pageHome.visibility = View.GONE
                    pageVault.visibility = View.VISIBLE
                    loadVaultData()
                    true
                }
                else -> false
            }
        }



        passwordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                passwordInputLayout.error = null
                if (s.isNullOrEmpty()) {
                    resetDashboard()
                }
            }
        })

        passwordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                analyzePassword()
                true
            } else {
                false
            }
        }
    }

    private fun loginFirebaseUser() {
        val auth = firebaseAuth
        if (!firebaseReady || auth == null) {
            setStatusText(tvAuthStatus, "Firebase belum aktif. Tambahkan app/google-services.json dari Firebase Console.", R.color.warning)
            return
        }

        val email = authEmailInput.textValue()
        val password = authPasswordInput.textValue()
        if (email.isBlank() || password.isBlank()) {
            setStatusText(tvAuthStatus, "Isi email dan password Firebase terlebih dahulu.", R.color.warning)
            return
        }

        setStatusText(tvAuthStatus, "Mencoba login ke Firebase...", R.color.text_secondary)
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                authPasswordInput.text?.clear()
                updateAuthStatus()
                updateAuthGate()
            }
            .addOnFailureListener { exception ->
                setStatusText(tvAuthStatus, "Login gagal: ${exception.localizedMessage}", R.color.danger)
                updateAuthGate()
            }
    }

    private fun registerFirebaseUser() {
        val auth = firebaseAuth
        if (!firebaseReady || auth == null) {
            setStatusText(tvAuthStatus, "Firebase belum aktif. Tambahkan app/google-services.json dari Firebase Console.", R.color.warning)
            return
        }

        val email = authEmailInput.textValue()
        val password = authPasswordInput.textValue()
        if (email.isBlank() || password.length < 6) {
            setStatusText(tvAuthStatus, "Email wajib diisi dan password Firebase minimal 6 karakter.", R.color.warning)
            return
        }

        setStatusText(tvAuthStatus, "Mendaftarkan akun Firebase...", R.color.text_secondary)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                authPasswordInput.text?.clear()
                updateAuthStatus()
                updateAuthGate()
            }
            .addOnFailureListener { exception ->
                setStatusText(tvAuthStatus, "Daftar gagal: ${exception.localizedMessage}", R.color.danger)
                updateAuthGate()
            }
    }

    private fun signOutFirebaseUser() {
        firebaseAuth?.signOut()
        authPasswordInput.text?.clear()
        passwordInput.text?.clear()
        updateAuthStatus()
        resetDashboard()
        updateAuthGate()
    }

    private fun updateAuthStatus() {
        if (!firebaseReady) {
            setStatusText(tvAuthStatus, "Firebase belum aktif. Tambahkan app/google-services.json, lalu rebuild aplikasi.", R.color.warning)
            return
        }

        val user = firebaseAuth?.currentUser
        if (user == null) {
            setStatusText(tvAuthStatus, "Firebase aktif. Login atau daftar untuk menggunakan aplikasi.", R.color.text_secondary)
        } else {
            setStatusText(tvAuthStatus, "Login sebagai ${user.email ?: user.uid}.", R.color.success)
        }
    }

    private fun updateAuthGate() {
        val isLoggedIn = firebaseReady && firebaseAuth?.currentUser != null
        authGate.visibility = if (isLoggedIn) View.GONE else View.VISIBLE
        pageContainer.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
        pageHome.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
        if (!isLoggedIn) pageVault.visibility = View.GONE
    }

    private fun analyzePassword() {
        val password = passwordInput.textValue()
        if (password.isBlank()) {
            passwordInputLayout.error = "Password belum diisi"
            resetDashboard()
            return
        }

        passwordInputLayout.error = null
        val report = createPasswordReport(password)
        renderReport(report)
    }

    private fun showSaveVaultDialog() {
        val password = passwordInput.textValue()
        if (password.isBlank()) {
            Toast.makeText(this, "Password kosong! Lakukan analisis terlebih dahulu.", Toast.LENGTH_SHORT).show()
            return
        }

        val report = createPasswordReport(password)
        
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save_vault, null)
        val etService = dialogView.findViewById<TextInputEditText>(R.id.etServiceName)
        val etUsername = dialogView.findViewById<TextInputEditText>(R.id.etUsername)

        AlertDialog.Builder(this)
            .setTitle("Simpan ke Vault")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val service = etService.text?.toString()?.trim() ?: ""
                val username = etUsername.text?.toString()?.trim() ?: ""
                if (service.isNotEmpty() && username.isNotEmpty()) {
                    saveToFirestore(service, username, report)
                } else {
                    Toast.makeText(this, "Layanan dan Username wajib diisi!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun saveToFirestore(serviceName: String, username: String, report: PasswordReport) {
        val user = firebaseAuth?.currentUser
        if (user == null || firestore == null) {
            Toast.makeText(this, "Gagal menyimpan. Pastikan sudah login.", Toast.LENGTH_SHORT).show()
            return
        }

        // Pendekatan Zero-Knowledge: Menggabungkan metadata dengan password asli untuk membuat fingerprint
        // dengan demikian plaintext password tidak dikirim ke server.
        val rawData = "VAULT|$serviceName|$username|${passwordInput.textValue()}"
        val fingerprint = sha256(rawData)

        val vaultData = hashMapOf(
            "serviceName" to serviceName,
            "username" to username,
            "strengthLevel" to report.level,
            "fingerprintSha256" to fingerprint,
            "createdAt" to System.currentTimeMillis()
        )

        firestore!!.collection("users").document(user.uid)
            .collection("vault_entries")
            .add(vaultData)
            .addOnSuccessListener {
                Toast.makeText(this, "Berhasil disimpan ke Vault", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadVaultData() {
        val user = firebaseAuth?.currentUser
        if (user == null || firestore == null) return

        firestore!!.collection("users").document(user.uid)
            .collection("vault_entries")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                
                val entries = snapshot.documents.mapNotNull { doc ->
                    val serviceName = doc.getString("serviceName") ?: ""
                    val username = doc.getString("username") ?: ""
                    val strengthLevel = doc.getString("strengthLevel") ?: ""
                    val fingerprint = doc.getString("fingerprintSha256") ?: ""
                    val createdAt = doc.getLong("createdAt") ?: 0L
                    
                    VaultEntry(
                        id = doc.id,
                        serviceName = serviceName,
                        username = username,
                        strengthLevel = strengthLevel,
                        fingerprintSha256 = fingerprint,
                        createdAt = createdAt
                    )
                }
                vaultAdapter.updateData(entries)
            }
    }


    private fun createPasswordReport(password: String): PasswordReport {
        val passwordLength = password.length
        val uppercaseCount = password.count { it.isUpperCase() }
        val lowercaseCount = password.count { it.isLowerCase() }
        val digitCount = password.count { it.isDigit() }
        val symbolCount = password.count { !it.isLetterOrDigit() }
        val uniqueCharacterCount = password.toSet().size

        val hasMinLength = passwordLength >= MINIMUM_RECOMMENDED_LENGTH
        val hasUppercase = uppercaseCount > 0
        val hasLowercase = lowercaseCount > 0
        val hasDigit = digitCount > 0
        val hasSymbol = symbolCount > 0
        val hasRepeatedCharacter = REPEATED_CHARACTER_PATTERN.containsMatchIn(password)
        val hasSequentialPattern = containsSequentialPattern(password)
        val hasCommonWord = containsCommonWord(password)
        val hasNoCommonPattern = !hasRepeatedCharacter && !hasSequentialPattern && !hasCommonWord

        val characterSetSize = calculateCharacterSetSize(
            hasUppercase = hasUppercase,
            hasLowercase = hasLowercase,
            hasDigit = hasDigit,
            hasSymbol = hasSymbol
        )
        val entropyBits = calculateEntropy(passwordLength, characterSetSize)
        val score = calculateStrengthScore(
            passwordLength = passwordLength,
            uniqueCharacterCount = uniqueCharacterCount,
            entropyBits = entropyBits,
            hasUppercase = hasUppercase,
            hasLowercase = hasLowercase,
            hasDigit = hasDigit,
            hasSymbol = hasSymbol,
            hasRepeatedCharacter = hasRepeatedCharacter,
            hasSequentialPattern = hasSequentialPattern,
            hasCommonWord = hasCommonWord
        )

        return PasswordReport(
            passwordLength = passwordLength,
            uppercaseCount = uppercaseCount,
            lowercaseCount = lowercaseCount,
            digitCount = digitCount,
            symbolCount = symbolCount,
            uniqueCharacterCount = uniqueCharacterCount,
            characterSetSize = characterSetSize,
            entropyBits = entropyBits,
            estimatedCrackTime = estimateCrackTime(entropyBits),
            md5Hash = md5(password),
            sha256Hash = sha256(password),
            sha512Hash = sha512(password),
            score = score,
            level = strengthLevel(score),
            feedback = createFeedback(
                hasMinLength = hasMinLength,
                hasUppercase = hasUppercase,
                hasLowercase = hasLowercase,
                hasDigit = hasDigit,
                hasSymbol = hasSymbol,
                hasRepeatedCharacter = hasRepeatedCharacter,
                hasSequentialPattern = hasSequentialPattern,
                hasCommonWord = hasCommonWord,
                score = score
            ),
            hasMinLength = hasMinLength,
            hasUppercase = hasUppercase,
            hasLowercase = hasLowercase,
            hasDigit = hasDigit,
            hasSymbol = hasSymbol,
            hasNoCommonPattern = hasNoCommonPattern
        )
    }

    private fun renderReport(report: PasswordReport) {
        findViewById<View>(R.id.resultContainer).visibility = View.VISIBLE
        val scoreColor = ContextCompat.getColor(this, colorForScore(report.score))
        progressStrength.setIndicatorColor(scoreColor)
        progressStrength.setProgress(report.score, true)

        tvStrengthLabel.text = report.level
        tvStrengthLabel.setTextColor(scoreColor)
        tvStrengthScore.text = "Skor: ${report.score}/100"
        tvFeedback.text = report.feedback
        tvAnalyzedAt.text = "Terakhir dianalisis: ${currentTimeText()}"
        tvLengthValue.text = "Panjang password: ${report.passwordLength} karakter"
        tvCompositionValue.text = "Komposisi: huruf besar ${report.uppercaseCount}, huruf kecil ${report.lowercaseCount}, angka ${report.digitCount}, simbol ${report.symbolCount}"
        tvEntropyValue.text = "Entropy perkiraan: ${formatDecimal(report.entropyBits)} bit dari ${report.characterSetSize} kemungkinan karakter; karakter unik ${report.uniqueCharacterCount}"
        tvGuessTimeValue.text = "Estimasi ketahanan brute force: ${report.estimatedCrackTime}"
        tvHashMd5Value.text = "MD5: ${report.md5Hash}"
        tvHashValue.text = "SHA-256: ${report.sha256Hash}"
        tvHashSha512Value.text = "SHA-512: ${report.sha512Hash}"

        setRuleText(tvRuleLength, "Minimal 12 karakter", report.hasMinLength)
        setRuleText(tvRuleUppercase, "Memiliki huruf besar", report.hasUppercase)
        setRuleText(tvRuleLowercase, "Memiliki huruf kecil", report.hasLowercase)
        setRuleText(tvRuleDigit, "Memiliki angka", report.hasDigit)
        setRuleText(tvRuleSymbol, "Memiliki simbol", report.hasSymbol)
        setRuleText(tvRulePattern, "Tidak memakai pola umum", report.hasNoCommonPattern)
    }

    private fun resetDashboard() {
        findViewById<View>(R.id.resultContainer).visibility = View.GONE
        val neutralColor = ContextCompat.getColor(this, R.color.text_secondary)
        progressStrength.setIndicatorColor(ContextCompat.getColor(this, R.color.primary))
        progressStrength.setProgress(0, false)
        tvStrengthLabel.text = "Belum dianalisis"
        tvStrengthLabel.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        tvStrengthScore.text = "Skor: 0/100"
        tvFeedback.text = "Masukkan password lalu tekan Analisis untuk melihat rekomendasi."
        tvAnalyzedAt.text = "Terakhir dianalisis: -"
        tvLengthValue.text = "Panjang password: 0 karakter"
        tvCompositionValue.text = "Komposisi: huruf besar 0, huruf kecil 0, angka 0, simbol 0"
        tvEntropyValue.text = "Entropy perkiraan: 0.0 bit"
        tvGuessTimeValue.text = "Estimasi ketahanan brute force: -"
        tvHashMd5Value.text = "MD5: -"
        tvHashValue.text = "SHA-256: -"
        tvHashSha512Value.text = "SHA-512: -"
        listOf(
            tvRuleLength to "Minimal 12 karakter",
            tvRuleUppercase to "Memiliki huruf besar",
            tvRuleLowercase to "Memiliki huruf kecil",
            tvRuleDigit to "Memiliki angka",
            tvRuleSymbol to "Memiliki simbol",
            tvRulePattern to "Tidak memakai pola umum"
        ).forEach { (view, label) ->
            view.text = "- $label"
            view.setTextColor(neutralColor)
        }
    }

    private fun calculateCharacterSetSize(
        hasUppercase: Boolean,
        hasLowercase: Boolean,
        hasDigit: Boolean,
        hasSymbol: Boolean
    ): Int {
        var characterSetSize = 0
        if (hasUppercase) characterSetSize += UPPERCASE_SET_SIZE
        if (hasLowercase) characterSetSize += LOWERCASE_SET_SIZE
        if (hasDigit) characterSetSize += DIGIT_SET_SIZE
        if (hasSymbol) characterSetSize += SYMBOL_SET_SIZE
        return characterSetSize
    }

    private fun calculateEntropy(passwordLength: Int, characterSetSize: Int): Double {
        if (passwordLength == 0 || characterSetSize == 0) return 0.0
        val logBaseTwo = ln(characterSetSize.toDouble()) / ln(2.0)
        return passwordLength * logBaseTwo
    }

    private fun calculateStrengthScore(
        passwordLength: Int,
        uniqueCharacterCount: Int,
        entropyBits: Double,
        hasUppercase: Boolean,
        hasLowercase: Boolean,
        hasDigit: Boolean,
        hasSymbol: Boolean,
        hasRepeatedCharacter: Boolean,
        hasSequentialPattern: Boolean,
        hasCommonWord: Boolean
    ): Int {
        val lengthScore = min(passwordLength * 4, 40)
        val varietyScore = listOf(hasUppercase, hasLowercase, hasDigit, hasSymbol).count { it } * 10
        val uniquenessScore = min(uniqueCharacterCount * 2, 12)
        val entropyBonus = when {
            entropyBits >= 80.0 -> 18
            entropyBits >= 60.0 -> 12
            entropyBits >= 40.0 -> 6
            else -> 0
        }
        val penalty = listOf(
            if (hasRepeatedCharacter) 12 else 0,
            if (hasSequentialPattern) 10 else 0,
            if (hasCommonWord) 18 else 0
        ).sum()

        return (lengthScore + varietyScore + uniquenessScore + entropyBonus - penalty).coerceIn(0, 100)
    }

    private fun containsSequentialPattern(password: String): Boolean {
        val normalizedPassword = password.lowercase(Locale.ROOT)
        if (normalizedPassword.length < 3) return false

        for (index in 0 until normalizedPassword.length - 2) {
            val firstCharacter = normalizedPassword[index]
            val secondCharacter = normalizedPassword[index + 1]
            val thirdCharacter = normalizedPassword[index + 2]
            val isLetterSequence = firstCharacter.isLetter() && secondCharacter.isLetter() && thirdCharacter.isLetter()
            val isDigitSequence = firstCharacter.isDigit() && secondCharacter.isDigit() && thirdCharacter.isDigit()
            if (!isLetterSequence && !isDigitSequence) continue

            val first = firstCharacter.code
            val second = secondCharacter.code
            val third = thirdCharacter.code
            if (second == first + 1 && third == second + 1) return true
            if (second == first - 1 && third == second - 1) return true
        }

        return KEYBOARD_PATTERNS.any { pattern ->
            normalizedPassword.contains(pattern)
        }
    }

    private fun containsCommonWord(password: String): Boolean {
        val normalizedPassword = password.lowercase(Locale.ROOT)
        return COMMON_PASSWORD_WORDS.any { word ->
            normalizedPassword.contains(word)
        }
    }

    private fun createFeedback(
        hasMinLength: Boolean,
        hasUppercase: Boolean,
        hasLowercase: Boolean,
        hasDigit: Boolean,
        hasSymbol: Boolean,
        hasRepeatedCharacter: Boolean,
        hasSequentialPattern: Boolean,
        hasCommonWord: Boolean,
        score: Int
    ): String {
        val suggestions = mutableListOf<String>()
        if (!hasMinLength) suggestions += "tambah panjang menjadi minimal 12 karakter"
        if (!hasUppercase) suggestions += "tambahkan huruf besar"
        if (!hasLowercase) suggestions += "tambahkan huruf kecil"
        if (!hasDigit) suggestions += "tambahkan angka"
        if (!hasSymbol) suggestions += "tambahkan simbol"
        if (hasRepeatedCharacter) suggestions += "hindari karakter berulang seperti aaa atau 111"
        if (hasSequentialPattern) suggestions += "hindari urutan seperti abc, 123, atau qwerty"
        if (hasCommonWord) suggestions += "hindari kata umum seperti password, admin, atau rahasia"

        return when {
            suggestions.isEmpty() && score >= 80 -> "Password sudah kuat untuk contoh aplikasi ini. Tetap gunakan password berbeda untuk setiap akun."
            suggestions.isEmpty() -> "Struktur password sudah memenuhi checklist dasar, tetapi frasa yang lebih panjang akan menaikkan entropy."
            else -> "Saran: ${suggestions.joinToString(separator = "; ")}."
        }
    }

    private fun estimateCrackTime(entropyBits: Double): String {
        if (entropyBits <= 0.0) return "-"
        if (entropyBits >= 90.0) return "> 1 juta tahun"

        val combinations = 2.0.pow(entropyBits)
        val averageSeconds = combinations / (2 * GUESSES_PER_SECOND)
        return when {
            averageSeconds < 1 -> "kurang dari 1 detik"
            averageSeconds < 60 -> "${averageSeconds.toInt()} detik"
            averageSeconds < 3600 -> "${(averageSeconds / 60).toInt()} menit"
            averageSeconds < 86400 -> "${(averageSeconds / 3600).toInt()} jam"
            averageSeconds < 31536000 -> "${(averageSeconds / 86400).toInt()} hari"
            averageSeconds < 3153600000 -> "${(averageSeconds / 31536000).toInt()} tahun"
            else -> "> 100 tahun"
        }
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { byte ->
            String.format(Locale.US, "%02x", byte.toInt() and 0xff)
        }
    }

    private fun md5(text: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { byte ->
            String.format(Locale.US, "%02x", byte.toInt() and 0xff)
        }
    }

    private fun sha512(text: String): String {
        val digest = MessageDigest.getInstance("SHA-512").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { byte ->
            String.format(Locale.US, "%02x", byte.toInt() and 0xff)
        }
    }

    private fun strengthLevel(score: Int): String {
        return when {
            score < 40 -> "Lemah"
            score < 70 -> "Sedang"
            score < 90 -> "Kuat"
            else -> "Sangat kuat"
        }
    }

    private fun colorForScore(score: Int): Int {
        return when {
            score < 40 -> R.color.danger
            score < 70 -> R.color.warning
            else -> R.color.success
        }
    }

    private fun setRuleText(view: TextView, label: String, isPassed: Boolean) {
        val colorRes = if (isPassed) R.color.success else R.color.warning
        val prefix = if (isPassed) "[OK]" else "[!]"
        view.text = "$prefix $label"
        view.setTextColor(ContextCompat.getColor(this, colorRes))
    }

    private fun setStatusText(view: TextView, text: String, colorRes: Int) {
        view.text = text
        view.setTextColor(ContextCompat.getColor(this, colorRes))
    }

    private fun TextInputEditText.textValue(): String {
        return text?.toString()?.trim().orEmpty()
    }

    private fun currentTimeText(): String {
        val localeIndonesia = Locale.forLanguageTag("id-ID")
        return SimpleDateFormat("HH:mm:ss", localeIndonesia).format(Date())
    }

    private fun formatDecimal(value: Double): String {
        return String.format(Locale.US, "%.1f", value)
    }

    private data class PasswordReport(
        val passwordLength: Int,
        val uppercaseCount: Int,
        val lowercaseCount: Int,
        val digitCount: Int,
        val symbolCount: Int,
        val uniqueCharacterCount: Int,
        val characterSetSize: Int,
        val entropyBits: Double,
        val estimatedCrackTime: String,
        val md5Hash: String,
        val sha256Hash: String,
        val sha512Hash: String,
        val score: Int,
        val level: String,
        val feedback: String,
        val hasMinLength: Boolean,
        val hasUppercase: Boolean,
        val hasLowercase: Boolean,
        val hasDigit: Boolean,
        val hasSymbol: Boolean,
        val hasNoCommonPattern: Boolean
    )

    private fun generateStrongPassword(): String {
        val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lower = "abcdefghijklmnopqrstuvwxyz"
        val digits = "0123456789"
        val symbols = "!@#$%^&*()-_=+<>?"
        val allChars = upper + lower + digits + symbols
        val random = SecureRandom()
        
        val passwordLength = 16
        val password = StringBuilder(passwordLength)
        
        password.append(upper[random.nextInt(upper.length)])
        password.append(lower[random.nextInt(lower.length)])
        password.append(digits[random.nextInt(digits.length)])
        password.append(symbols[random.nextInt(symbols.length)])
        
        for (i in 4 until passwordLength) {
            password.append(allChars[random.nextInt(allChars.length)])
        }
        
        val passwordChars = password.toString().toCharArray()
        for (i in passwordChars.indices) {
            val j = random.nextInt(passwordChars.size)
            val temp = passwordChars[i]
            passwordChars[i] = passwordChars[j]
            passwordChars[j] = temp
        }
        
        return String(passwordChars)
    }

    private companion object {
        const val MINIMUM_RECOMMENDED_LENGTH = 12
        const val UPPERCASE_SET_SIZE = 26
        const val LOWERCASE_SET_SIZE = 26
        const val DIGIT_SET_SIZE = 10
        const val SYMBOL_SET_SIZE = 33
        const val GUESSES_PER_SECOND = 1_000_000_000.0

        val REPEATED_CHARACTER_PATTERN = Regex("(.)\\1{2,}")
        val KEYBOARD_PATTERNS = listOf(
            "qwerty", "wertyu", "ertyui", "rtyuio", "tyuiop",
            "asdfgh", "sdfghj", "dfghjk", "fghjkl",
            "zxcvbn", "xcvbnm",
            "123456", "234567", "345678", "456789", "567890",
            "987654", "876543", "765432", "654321",
            "qazwsx", "wsxedc", "edcrfv", "rfvtgb", "tgbyhn", "yhnujm", "ujmik"
        )
        val COMMON_PASSWORD_WORDS = listOf(
            "password", "admin", "rahasia", "sandi", "indonesia", "bismillah",
            "welcome", "iloveyou", "letmein", "123456", "qwerty", "login",
            "user", "guest", "test", "root", "super", "master", "sayang",
            "cinta", "kucing", "anjing", "monyet", "garuda", "merahputih",
            "jakarta", "bandung", "surabaya", "medan", "makassar"
        )
    }
}
