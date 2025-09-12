package com.agalplataformaeducativa.webview

import android.view.View
import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.webkit.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.FirebaseApp
import android.webkit.URLUtil
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.agalplataformaeducativa.webview.databinding.ActivityMainBinding
import com.agalplataformaeducativa.webview.databinding.LayoutNoInternetBinding
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import android.util.Log
import android.text.SpannableString
import android.text.style.UnderlineSpan

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager
    private var doubleBackToExitPressedOnce = false

    private lateinit var adView: AdView
    private var interstitialAd: InterstitialAd? = null

    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        Log.d("MainActivity", "onCreate() called") // ✅ LOG 1

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("MainActivity", "setContentView() done") // ✅ LOG 2

        prefs = PreferencesManager(this)
        FirebaseApp.initializeApp(this)

        adView = binding.adView
        loadBannerAd()
        setupFilePicker()

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            requestStoragePermission()
        }

        loadInterstitialAd()

        if (prefs.isFirstLaunch || prefs.savedSubdomain.isNullOrBlank()) {
            showSubdomainDialog()
        } else {
            loadWebView()
        }


    }

    override fun onStart() {
        super.onStart()
        if (!prefs.isFirstLaunch) {
            Handler(Looper.getMainLooper()).postDelayed({
                showInterstitialThenLoadWebView()
            }, 2000) // Pequeño retraso para asegurar que todo esté listo
        }
    }



    private fun setupFilePicker() {
        filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                filePathCallback?.onReceiveValue(result.data?.data?.let { arrayOf(it) })
                filePathCallback = null
            } else {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = null
            }
        }
    }

    private fun loadBannerAd() {
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            this,
            getString(R.string.admob_interstitial_id),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Ad failed to load: ${error.message}")
                    interstitialAd = null
                }
            }
        )
    }

    private fun showInterstitialThenLoadWebView() {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadWebView() // ✅ Cargar WebView DESPUÉS de cerrar el anuncio
                    loadInterstitialAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    loadWebView() // ✅ Cargar WebView SIEMPRE, incluso si falla el anuncio
                    loadInterstitialAd()
                }

                override fun onAdShowedFullScreenContent() {
                    // Anuncio mostrado
                }
            }
            interstitialAd?.show(this)
        } else {
            loadWebView() // ✅ ¡CARGAR WEBVIEW SIEMPRE!
            loadInterstitialAd()
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Permiso necesario para acceder a archivos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSubdomainDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_subdomain, null)
        val editText = dialogView.findViewById<EditText>(R.id.editTextSubdomain)
        val checkboxTerms = dialogView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.checkboxTerms)
        val textViewTermsLink = dialogView.findViewById<com.google.android.material.textview.MaterialTextView>(R.id.textViewTermsLink)

        // ✅ Abrir política de privacidad en el navegador
        textViewTermsLink.setOnClickListener {
            val url = "https://agalplataformaeducativa.com/politica_privacidad.html"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        // ✅ Aplicar subrayado al texto
        val termsText = getString(R.string.terms_link)
        val spannableString = SpannableString(termsText)
        spannableString.setSpan(UnderlineSpan(), 0, termsText.length, 0)
        textViewTermsLink.text = spannableString


        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                // ✅ Verificar que se haya marcado el checkbox
                if (!checkboxTerms.isChecked) {
                    Toast.makeText(this, "Debes aceptar los términos y condiciones", Toast.LENGTH_SHORT).show()
                    showSubdomainDialog() // Volver a mostrar el diálogo
                    return@setPositiveButton
                }

                val input = editText.text.toString().trim()
                if (isValidSubdomain(input)) {
                    // ✅ Validar que el subdominio responda antes de guardarlo
                    validateAndSaveSubdomain(input, editText)
                } else {
                    Toast.makeText(this, R.string.invalid_domain, Toast.LENGTH_SHORT).show()
                    showSubdomainDialog()
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun validateAndSaveSubdomain(subdomain: String, editText: EditText) {
        val url = "https://$subdomain"

        Thread {
            try {
                val connection = java.net.URL(url).openConnection()
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.connect()

                runOnUiThread {
                    prefs.savedSubdomain = subdomain
                    prefs.isFirstLaunch = false
                    loadInterstitialAd()
                    Handler(Looper.getMainLooper()).postDelayed({
                        showInterstitialThenLoadWebView()
                    }, 1000)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "El subdominio no está disponible. Verifica e intenta de nuevo.", Toast.LENGTH_LONG).show()
                    editText.setText("")
                    showSubdomainDialog()
                }
            }
        }.start()
    }

    private fun isValidSubdomain(input: String): Boolean {
        if (input.isEmpty()) return false
        if (!input.endsWith(".agalplataformaeducativa.com")) return false
        if (input == "agalplataformaeducativa.com") return false
        return true
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadWebView() {
        Log.d("WebView", "loadWebView() called")

        val subdomain = prefs.savedSubdomain ?: return
        val url = "https://$subdomain"

        Log.d("WebView", "Loading URL: $url")

        binding.webview.visibility = View.VISIBLE

        binding.webview.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            mediaPlaybackRequiresUserGesture = false
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT // ✅ Cambiar a LOAD_DEFAULT
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setAcceptThirdPartyCookies(binding.webview, true)
            }
        }

        binding.webview.clearCache(true)
        binding.webview.clearHistory()

        binding.swipeRefresh.setOnRefreshListener {
            binding.webview.reload()
        }

        binding.webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false // ✅ Dejar que el WebView maneje todo
            }
        }

        // ✅ Cargar la URL directamente
        binding.webview.loadUrl(url)
    }

    private fun downloadFile(url: String, userAgent: String, contentDisposition: String, mimeType: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
            request.setMimeType(mimeType)
            request.addRequestHeader("User-Agent", userAgent)
            request.setDescription("Descargando archivo...")
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType))

            val dm = getSystemService<DownloadManager>()
            dm?.enqueue(request)

            Toast.makeText(this, "Descarga iniciada", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al iniciar descarga", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isOnline(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                    capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        } else {
            val activeNetwork = connectivityManager.activeNetworkInfo
            activeNetwork != null && activeNetwork.isConnected
        }
    }

    private fun showNoInternetScreen() {
        val noInternetBinding = LayoutNoInternetBinding.inflate(layoutInflater)
        binding.webview.removeAllViews()
        binding.webview.addView(noInternetBinding.root)

        noInternetBinding.btnRetry.setOnClickListener {
            if (isOnline()) {
                binding.webview.removeAllViews()
                loadWebView()
            } else {
                Toast.makeText(this, "Aún no hay conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }



    override fun onBackPressed() {
        if (binding.webview.canGoBack()) {
            binding.webview.goBack()
        } else {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed()
                return
            }
            doubleBackToExitPressedOnce = true
            Toast.makeText(this, R.string.exit_message, Toast.LENGTH_SHORT).show()
            Handler(Looper.getMainLooper()).postDelayed({
                doubleBackToExitPressedOnce = false
            }, 2000)
        }
    }

    override fun onDestroy() {
        if (::adView.isInitialized) {
            adView.destroy()
        }
        super.onDestroy()
    }



    companion object {
        const val FILE_CHOOSER_REQUEST_CODE = 100
    }
}