package com.chanfan.getyourclassschedule

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.net_mode_fragment.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.concurrent.thread

class NetModeFragment : Fragment() {
    private lateinit var loginService: LoginService
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.net_mode_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getCodePic.setOnClickListener {
            loginService = ServiceCreator.create(LoginService::class.java)
            loginService.get("https://sso.scnu.edu.cn/AccountService/user/rancode.jpg")
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        val inputStream = response.body()?.byteStream()
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        codeImg.setImageBitmap(bitmap)
                        inputStream?.close()
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Toast.makeText(context, "网络好像出了问题呢~", Toast.LENGTH_SHORT).show()
                    }
                })
        }
        login.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    GlobalApp.context,
                    Manifest.permission.WRITE_CALENDAR
                )
                != PackageManager.PERMISSION_GRANTED
                ||
                ContextCompat.checkSelfPermission(
                    GlobalApp.context,
                    Manifest.permission.READ_CALENDAR
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.WRITE_CALENDAR,
                        Manifest.permission.READ_CALENDAR
                    ), 1
                )
            } else {
                if (activity != null) {
                    val mainActivity = activity as MainActivity
                    mainActivity.loadingDialog.show()
                    writeCalendar()
                    mainActivity.loadingDialog.dismiss()
                    Toast.makeText(context, "写入完成", Toast.LENGTH_SHORT).show()
                    mainActivity.shareDialog.show()
                }
            }
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                var allGrant = false
                for (i in 0 until 2) {
                    if (grantResults.isNotEmpty() && grantResults[i] == PackageManager.PERMISSION_GRANTED)
                        allGrant = true
                    else {
                        allGrant = false
                        Toast.makeText(context, "权限被拒绝了呢~", Toast.LENGTH_SHORT).show()
                    }
                }
                if (allGrant) {
                    writeCalendar()
                } else {
                    Toast.makeText(context, "权限没给够哦", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun writeCalendar() {
        val ac = account.text.toString()
        val pw = password.text.toString()
        val rc = verifyCode.text.toString()
        if (ac == "" || pw == "" || rc == "") {
            Toast.makeText(context, "请输入账号信息", Toast.LENGTH_SHORT).show()
        } else {
            thread {
                val loginForm = mapOf(
                    "account" to ac,
                    "password" to pw,
                    "rancode" to rc,
                    "client_id" to "",
                    "response_type" to "",
                    "redirect_url" to "",
                    "jump" to ""
                )
                //异步会发生顺序错误导致无法登陆
                loginService.post(
                    loginForm,
                    "https://sso.scnu.edu.cn/AccountService/user/login.html"
                ).execute()
                loginService.get("https://sso.scnu.edu.cn/AccountService/openapi/onekeyapp.html?id=96")
                    .execute()
                val formData = mapOf("xnm" to "2020", "xqm" to "3")
                val classData = loginService.post(
                    formData,
                    "https://jwxt.scnu.edu.cn/kbcx/xskbcx_cxXsKb.html?gnmkdm=N253508"
                ).execute().body()?.string()
                if (classData != null)
                    if (SHIPAI.isChecked)
                        ClassTableICAL.handleTextData(classData, ClassTableICAL.SHIPAI)
                    else
                        ClassTableICAL.handleTextData(classData, ClassTableICAL.NANHAI)
            }
        }
    }
}