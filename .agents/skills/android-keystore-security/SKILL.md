---
name: android-keystore-security
description: Hardware-backed cryptography, Android Keystore management, EncryptedSharedPreferences, Biometric authentication with CryptoObject, and OkHttp/Ktor certificate pinning.
---

# Android Keystore, Cryptography & Hardware Security

This skill establishes zero-trust mobile security, hardware-backed encryption, and secure authentication standards.

---

## 1. Android KeyStore AES-256 GCM Encryption

Generate hardware-backed cryptographic keys inside the Secure Element (StrongBox) or Trusted Execution Environment (TEE):

```kotlin
class CryptoManager {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private fun getKey(): SecretKey {
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createKey()
    }

    private fun createKey(): SecretKey {
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setUserAuthenticationRequired(false) // Set to true if gated by Biometrics
                    .setIsStrongBoxBacked(false) // Set to true on supported hardware
                    .build()
            )
        }.generateKey()
    }

    fun encrypt(bytes: ByteArray, outputStream: OutputStream): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        val iv = cipher.iv
        outputStream.write(iv.size)
        outputStream.write(iv)
        val encryptedBytes = cipher.doFinal(bytes)
        outputStream.write(encryptedBytes)
        return encryptedBytes
    }

    fun decrypt(inputStream: InputStream): ByteArray {
        val ivSize = inputStream.read()
        val iv = ByteArray(ivSize)
        inputStream.read(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)
        val encryptedBytes = inputStream.readBytes()
        return cipher.doFinal(encryptedBytes)
    }

    companion object {
        private const val KEY_ALIAS = "vennela_vault_key"
    }
}
```

---

## 2. Jetpack Security (EncryptedSharedPreferences)

Store sensitive user preferences, tokens, and encryption keys securely:

```kotlin
fun createEncryptedPreferences(context: Context): SharedPreferences {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    return EncryptedSharedPreferences.create(
        context,
        "secure_vault_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}
```

---

## 3. BiometricPrompt with CryptoObject

Require biometric authentication to unlock cryptographic keys:

```kotlin
fun showBiometricPrompt(
    activity: FragmentActivity,
    cryptoObject: BiometricPrompt.CryptoObject,
    onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
    onError: (Int, CharSequence) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess(result)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errorCode, errString)
            }
        }
    )

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Authenticate to Unlock")
        .setSubtitle("Confirm your biometric identity to access secure files")
        .setNegativeButtonText("Cancel")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        .build()

    prompt.authenticate(promptInfo, cryptoObject)
}
```

---

## 4. Certificate Pinning (Prevent MITM Attacks)

Pin public key hashes for all backend network domains:

```kotlin
val certificatePinner = CertificatePinner.Builder()
    .add("api.vennela365.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
    .add("api.vennela365.com", "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=") // Backup pin
    .build()

val okHttpClient = OkHttpClient.Builder()
    .certificatePinner(certificatePinner)
    .build()
```

