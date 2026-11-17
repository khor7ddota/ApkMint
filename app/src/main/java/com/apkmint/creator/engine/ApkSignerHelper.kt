package com.apkmint.creator.engine

import android.content.Context
import com.android.apksig.ApkSigner
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date

object ApkSignerHelper {

    private const val KEYSTORE_FILE = "apkmint_keystore.p12"
    private const val KEY_ALIAS = "apkmint"
    private val KEY_PASS = "apkmint123".toCharArray()

    fun signApk(context: Context, unsignedApk: File, signedApk: File) {
        val (privateKey, cert) = loadOrCreateKeyPair(context)
        val signerConfig = ApkSigner.SignerConfig.Builder(
            KEY_ALIAS,
            privateKey,
            listOf(cert)
        ).build()

        val signer = ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(unsignedApk)
            .setOutputApk(signedApk)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .setMinSdkVersion(24)
            .build()

        signer.sign()
    }

    private fun loadOrCreateKeyPair(context: Context): Pair<PrivateKey, X509Certificate> {
        val ksFile = File(context.filesDir, KEYSTORE_FILE)
        val keyStore = KeyStore.getInstance("PKCS12")

        if (ksFile.exists()) {
            runCatching {
                ksFile.inputStream().use { keyStore.load(it, KEY_PASS) }
                val key = keyStore.getKey(KEY_ALIAS, KEY_PASS) as PrivateKey
                val cert = keyStore.getCertificate(KEY_ALIAS) as X509Certificate
                return key to cert
            }
        }

        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048, SecureRandom())
        val keyPair = kpg.generateKeyPair()

        val now = System.currentTimeMillis()
        val notBefore = Date(now - 86_400_000L)
        val notAfter = Date(now + 30L * 365 * 86_400_000L)
        val serial = BigInteger(64, SecureRandom()).abs()
        val owner = X500Name("CN=ApkMint Builder, O=ApkMint, C=BD")

        val certBuilder = JcaX509v3CertificateBuilder(
            owner,
            serial,
            notBefore,
            notAfter,
            owner,
            keyPair.public
        )
        val contentSigner = JcaContentSignerBuilder("SHA256WithRSAEncryption")
            .build(keyPair.private)
        val certHolder = certBuilder.build(contentSigner)
        val cert = JcaX509CertificateConverter().getCertificate(certHolder)

        keyStore.load(null, null)
        keyStore.setKeyEntry(KEY_ALIAS, keyPair.private, KEY_PASS, arrayOf(cert))
        ksFile.outputStream().use { keyStore.store(it, KEY_PASS) }

        return keyPair.private to cert
    }
}
