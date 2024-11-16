package com.nudriin.mywebview

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintJob
import android.print.PrintManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.nudriin.mywebview.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val printJobs = mutableListOf<PrintJob>()

    inner class WebAppInterface(private val context: Context) {
        @JavascriptInterface
        fun printPage() {
            runOnUiThread {
                createWebPrintJob(binding.webView)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val webView = binding.webView
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true  // Aktifkan DOM storage jika diperlukan
        }

        // Tambahkan JavaScript interface
        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Inject JavaScript untuk menangkap event print
                injectPrintCatcher()
            }
        }

        webView.webChromeClient = WebChromeClient()

        webView.loadUrl("https://antrihelu.nudriin.space")
    }

    private fun injectPrintCatcher() {
        // JavaScript untuk menangkap event window.print()
        val javascript = """
            (function() {
                var originalPrint = window.print;
                window.print = function() {
                    Android.printPage();
                };
            })();
        """.trimIndent()

        binding.webView.evaluateJavascript(javascript, null)
    }

    private fun createWebPrintJob(webView: WebView) {
        (getSystemService(Context.PRINT_SERVICE) as? PrintManager)?.let { printManager ->
            val jobName = "${getString(R.string.app_name)} Document"
            val printAdapter = webView.createPrintDocumentAdapter(jobName)

            val printJob: PrintJob = printManager.print(
                jobName,
                printAdapter,
                PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A7)
                    .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
            )

            printJobs += printJob
        }
    }
}