package dev.tsnanh.vku.domain.handler

import android.util.Log
import dev.tsnanh.vku.domain.entities.Resource
import retrofit2.HttpException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.SocketTimeoutException

class ErrorHandler {
    companion object {
        fun <T> handleError(e: Throwable): Resource.Error<T> {
            Log.e("ERROR", e.message!!)
            return when (e) {
                is HttpException -> {
                    when (e.code()) {
                        HttpURLConnection.HTTP_NOT_FOUND -> Resource.Error("Not Found!")
                        HttpURLConnection.HTTP_UNAVAILABLE -> Resource.Error("Unavailable!")
                        HttpURLConnection.HTTP_BAD_REQUEST -> Resource.Error("Bad Request!")
                        else -> Resource.Error("Unknown Error!")
                    }
                }
                is SocketException -> Resource.Error("No Internet Connection!")
                is SocketTimeoutException -> Resource.Error("Request timeout!")
                is ConnectException -> Resource.Error("Cannot connect to server!")
                else -> Resource.Error("Unknown Error!")
            }
        }
    }
}