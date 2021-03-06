package com.example.wallet_connect_flutter


import android.content.Intent
import android.net.Uri

import android.util.Log
import androidx.annotation.NonNull
import androidx.multidex.MultiDexApplication
import com.example.wallet_connect_flutter.ExampleApplication
import org.walletconnect.Session

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import org.walletconnect.nullOnThrow



class MainActivity: FlutterActivity() , Session.Callback {


    private var txRequest: Long? = null
    private val CHANNEL = "com.accubits.flutter/wallet_connect_info"

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler {

            // Note: this method is invoked on the main thread.
            call, result ->
            if (call.method == "connectToWallet") {
                try {
                    initialSetup()
                    ExampleApplication.resetSession()
                    ExampleApplication.session.addCallback(this)
                } catch (e: UninitializedPropertyAccessException) {
                    Log.d("#####", "in connect wallet catch block")
                } finally {
                    Log.d("#####", "in finally block 01")
                }
            } else if(call.method == "getAccounts") {
                try {
                    Log.d("#####", "in try block")
                    result.success(ExampleApplication.session.approvedAccounts())
                } catch (e: UninitializedPropertyAccessException) {
                    result.success("")
                } finally {
                    Log.d("#####", "in finally block")
                }
            }
            else if(call.method == "sendTransaction") {
                val from = ExampleApplication.session.approvedAccounts()?.first()
                        ?: return@setMethodCallHandler
                val txRequest = System.currentTimeMillis()
                ExampleApplication.session.performMethodCall(
                        Session.MethodCall.SendTransaction(
                                txRequest,
                                from,
                                "0x24EdA4f7d0c466cc60302b9b5e9275544E5ba552",
                                null,
                                null,
                                null,
                                "0x5AF3107A4000",
                                ""
                        ),
                        ::handleResponse
                )
                this.txRequest = txRequest
                navigateToWallet()
            }
            else {
                result.notImplemented()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        initialSetup()
    }

    private fun requestConnectionToWallet() {
        try {
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(ExampleApplication.config.toWCUri())
            startActivity(i)
        } catch ( e : Exception){
            Log.d("#####", "unable to launch activity")
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=io.metamask")))
        }
    }

    private fun navigateToWallet() {
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse("wc:")
        startActivity(i)
    }

    override fun onMethodCall(call: Session.MethodCall) {
        TODO("Not yet implemented")
    }

    override fun onStatus(status: Session.Status) {
        when(status) {
            Session.Status.Approved -> sessionApproved()
            Session.Status.Closed -> sessionClosed()
            Session.Status.Connected -> {
                requestConnectionToWallet()
            }
            Session.Status.Disconnected,
            is Session.Status.Error -> {
                // Do Stuff
            }
        }
    }

    private fun handleResponse(resp: Session.MethodCall.Response) {
        if (resp.id == txRequest) {
            txRequest = null
            Log.d("#####", "Last response: + ${(resp.result as? String)} ?: Unknown response")
        }
    }


    private fun sessionApproved() {
        Log.d("#####", "Connected: ${ExampleApplication.session.approvedAccounts()}")
    }

    private fun sessionClosed() {
        Log.d("#####", "closed")
    }

    private fun initialSetup() {
        val session = nullOnThrow { ExampleApplication.session } ?: return
        session.addCallback(this)
        sessionApproved()
    }

    override fun onDestroy() {
        ExampleApplication.session.removeCallback(this)
        super.onDestroy()
    }


}
