package com.example.myaicalc

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalorieCounterApp(this)
        }
    }
}

@Composable
fun CalorieCounterApp(context: Context) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var calories by remember { mutableStateOf<String?>(null) }
    var protein by remember { mutableStateOf<String?>(null) }
    var fat by remember { mutableStateOf<String?>(null) }
    var carbs by remember { mutableStateOf<String?>(null) }
    var weight by remember { mutableStateOf("70") }
    var dailyCalorieGoal by remember { mutableStateOf("2000") }
    var trainingCalories by remember { mutableStateOf("0") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }
    val foodLog = remember { mutableStateListOf<String>() }
    val weightHistory = remember { mutableStateListOf<Double>() }
    val trainingLog = remember { mutableStateListOf<String>() }

    Column(modifier = Modifier.padding(16.dp)) {
        TextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text("Ваш вес (кг)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            dailyCalorieGoal = calculateCalorieGoal(weight.toDoubleOrNull() ?: 70.0).toString()
            weightHistory.add(weight.toDoubleOrNull() ?: 70.0)
        }) {
            Text("Рассчитать норму калорий")
        }
        Text("Дневная норма: $dailyCalorieGoal ккал")
        Spacer(modifier = Modifier.height(16.dp))
        Text("Добавить тренировку:")
        Button(onClick = {
            val burnedCalories = 300
            trainingCalories = burnedCalories.toString()
            trainingLog.add("Бег - 30 мин: $burnedCalories ккал")
        }) {
            Text("Добавить бег 30 мин")
        }
        Text("Сожженные калории: $trainingCalories ккал")
        Spacer(modifier = Modifier.height(16.dp))
        Text("История тренировок:")
        LazyColumn {
            items(trainingLog) { item ->
                Text(item)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("История приёмов пищи:")
        LazyColumn {
            items(foodLog) { item ->
                Text(item)
            }
        }
    }
}

fun calculateCalorieGoal(weight: Double): Int {
    return (weight * 30).toInt()
}

fun uploadImage(uri: Uri, context: Context, onResult: (String, String, String, String) -> Unit) {
    val file = File.createTempFile("upload", "jpg", context.cacheDir)
    val inputStream = context.contentResolver.openInputStream(uri)
    file.outputStream().use { outputStream -> inputStream?.copyTo(outputStream) }

    val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
    val multipartBody = MultipartBody.Part.createFormData("file", file.name, requestBody)

    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.example.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service = retrofit.create(ApiService::class.java)
    service.uploadImage(multipartBody).enqueue(object : retrofit2.Callback<ApiResponse> {
        override fun onResponse(call: retrofit2.Call<ApiResponse>, response: retrofit2.Response<ApiResponse>) {
            if (response.isSuccessful) {
                val data = response.body()
                onResult(data?.calories ?: "Ошибка", data?.protein ?: "0", data?.fat ?: "0", data?.carbs ?: "0")
            }
        }
        override fun onFailure(call: retrofit2.Call<ApiResponse>, t: Throwable) {
            Log.e("Upload", "Ошибка загрузки", t)
        }
    })
}

interface ApiService {
    @Multipart
    @POST("recognize")
    fun uploadImage(@Part image: MultipartBody.Part): retrofit2.Call<ApiResponse>
}

data class ApiResponse(val calories: String, val protein: String, val fat: String, val carbs: String)
