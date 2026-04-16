package org.thebytearray.app.android.openloader.core.adb

import android.content.Context
import android.os.Build
import android.sun.security.x509.AlgorithmId
import android.sun.security.x509.CertificateAlgorithmId
import android.sun.security.x509.CertificateExtensions
import android.sun.security.x509.CertificateIssuerName
import android.sun.security.x509.CertificateSerialNumber
import android.sun.security.x509.CertificateSubjectName
import android.sun.security.x509.CertificateValidity
import android.sun.security.x509.CertificateVersion
import android.sun.security.x509.CertificateX509Key
import android.sun.security.x509.KeyIdentifier
import android.sun.security.x509.PrivateKeyUsageExtension
import android.sun.security.x509.SubjectKeyIdentifierExtension
import android.sun.security.x509.X500Name
import android.sun.security.x509.X509CertImpl
import android.sun.security.x509.X509CertInfo
import android.util.Base64
import androidx.datastore.preferences.core.edit
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.thebytearray.app.android.openloader.core.datastore.AdbKeysPreferenceKeys
import org.thebytearray.app.android.openloader.core.datastore.openLoaderAdbKeysDataStore
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import java.util.Random

class AdbConnectionManager private constructor(private val context: Context) : AbsAdbConnectionManager() {

    companion object {
        @Volatile
        private var INSTANCE: AdbConnectionManager? = null

        fun getInstance(context: Context): AdbConnectionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AdbConnectionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private var mPrivateKey: PrivateKey? = null
    private var mCertificate: Certificate? = null
    private val dataStore = context.openLoaderAdbKeysDataStore

    init {
        setApi(Build.VERSION.SDK_INT)
        setThrowOnUnauthorised(true)
        loadOrGenerateKeys()
    }

    private fun loadOrGenerateKeys() {
        val (privateKeyStr, certStr) = runBlocking(Dispatchers.IO) {
            val prefs = dataStore.data.first()
            prefs[AdbKeysPreferenceKeys.PRIVATE_KEY] to prefs[AdbKeysPreferenceKeys.CERTIFICATE]
        }

        if (privateKeyStr != null && certStr != null) {
            try {
                mPrivateKey = loadPrivateKey(privateKeyStr)
                mCertificate = loadCertificate(certStr)
                return
            } catch (_: Exception) {
            }
        }

        generateAndSaveKeys()
    }

    private fun generateAndSaveKeys() {
        try {
            val keySize = 2048
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(keySize, SecureRandom.getInstance("SHA1PRNG"))
            val keyPair = keyPairGenerator.generateKeyPair()
            val publicKey = keyPair.public
            mPrivateKey = keyPair.private

            val subject = "CN=OpenLoader"
            val algorithmName = "SHA512withRSA"
            val expiryDate = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000 * 10)

            val certificateExtensions = CertificateExtensions()
            certificateExtensions.set(
                "SubjectKeyIdentifier",
                SubjectKeyIdentifierExtension(KeyIdentifier(publicKey).identifier),
            )

            val x500Name = X500Name(subject)
            val notBefore = Date()
            val notAfter = Date(expiryDate)

            certificateExtensions.set("PrivateKeyUsage",
                PrivateKeyUsageExtension(notBefore, notAfter)
            )
            val certificateValidity = CertificateValidity(notBefore, notAfter)

            val x509CertInfo = X509CertInfo()
            x509CertInfo.set("version", CertificateVersion(2))
            x509CertInfo.set("serialNumber",
                CertificateSerialNumber(Random().nextInt() and Integer.MAX_VALUE)
            )
            x509CertInfo.set("algorithmID", CertificateAlgorithmId(AlgorithmId.get(algorithmName)))
            x509CertInfo.set("subject", CertificateSubjectName(x500Name))
            x509CertInfo.set("key", CertificateX509Key(publicKey))
            x509CertInfo.set("validity", certificateValidity)
            x509CertInfo.set("issuer", CertificateIssuerName(x500Name))
            x509CertInfo.set("extensions", certificateExtensions)

            val x509CertImpl = X509CertImpl(x509CertInfo)
            x509CertImpl.sign(mPrivateKey, algorithmName)
            mCertificate = x509CertImpl

            saveKeys()
        } catch (e: Exception) {
            throw RuntimeException("Failed to generate ADB keys", e)
        }
    }

    private fun saveKeys() {
        runBlocking(Dispatchers.IO) {
            dataStore.edit { prefs ->
                prefs[AdbKeysPreferenceKeys.PRIVATE_KEY] =
                    Base64.encodeToString(mPrivateKey!!.encoded, Base64.DEFAULT)
                prefs[AdbKeysPreferenceKeys.CERTIFICATE] =
                    Base64.encodeToString(mCertificate!!.encoded, Base64.DEFAULT)
            }
        }
    }

    private fun loadPrivateKey(encoded: String): PrivateKey {
        val keyBytes = Base64.decode(encoded, Base64.DEFAULT)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePrivate(PKCS8EncodedKeySpec(keyBytes))
    }

    private fun loadCertificate(encoded: String): Certificate {
        val certBytes = Base64.decode(encoded, Base64.DEFAULT)
        val certFactory = CertificateFactory.getInstance("X.509")
        return certFactory.generateCertificate(certBytes.inputStream())
    }

    public override fun getPrivateKey(): PrivateKey = mPrivateKey!!

    public override fun getCertificate(): Certificate = mCertificate!!

    public override fun getDeviceName(): String = "OpenLoader"
}