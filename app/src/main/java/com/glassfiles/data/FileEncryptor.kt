package com.glassfiles.data

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object FileEncryptor {

    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val KEY_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val KEY_LENGTH = 256
    private const val ITERATIONS = 65536
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 16

    fun encrypt(inputFile: File, password: String): Result<File> {
        return try {
            val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
            val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
            val key = deriveKey(password, salt)

            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))

            val outputFile = File(inputFile.parent, "${inputFile.name}.enc")
            FileOutputStream(outputFile).use { fos ->
                fos.write(salt)
                fos.write(iv)
                FileInputStream(inputFile).use { fis ->
                    val buf = ByteArray(8192)
                    while (true) {
                        val n = fis.read(buf)
                        if (n <= 0) break
                        fos.write(cipher.update(buf, 0, n))
                    }
                    fos.write(cipher.doFinal())
                }
            }
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun decrypt(inputFile: File, password: String): Result<File> {
        return try {
            FileInputStream(inputFile).use { fis ->
                val salt = ByteArray(SALT_LENGTH); fis.read(salt)
                val iv = ByteArray(IV_LENGTH); fis.read(iv)
                val key = deriveKey(password, salt)

                val cipher = Cipher.getInstance(ALGORITHM)
                cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

                val outName = if (inputFile.name.endsWith(".enc")) inputFile.name.removeSuffix(".enc") else "${inputFile.nameWithoutExtension}_decrypted.${inputFile.extension}"
                val outputFile = File(inputFile.parent, outName)
                FileOutputStream(outputFile).use { fos ->
                    val buf = ByteArray(8192)
                    while (true) {
                        val n = fis.read(buf)
                        if (n <= 0) break
                        fos.write(cipher.update(buf, 0, n))
                    }
                    fos.write(cipher.doFinal())
                }
                Result.success(outputFile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance(KEY_ALGORITHM)
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val secret = factory.generateSecret(spec)
        return SecretKeySpec(secret.encoded, "AES")
    }
}
