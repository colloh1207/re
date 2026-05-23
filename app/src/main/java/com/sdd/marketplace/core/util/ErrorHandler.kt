package com.sdd.marketplace.core.util

object ErrorHandler {

    fun friendlyMessage(throwable: Throwable): String = friendlyMessage(throwable.message)

    fun friendlyMessage(raw: String?): String {
        if (raw.isNullOrBlank()) return "Something went wrong. Please try again."
        val msg = raw.lowercase()
        return when {
            msg.contains("connection reset") || msg.contains("connection refused") ||
            msg.contains("failed to connect") || msg.contains("unable to resolve host") ||
            msg.contains("no route to host") -> "Unable to connect. Please check your internet connection."

            msg.contains("timeout") || msg.contains("timed out") ->
                "Request timed out. Please try again."

            msg.contains("network") || msg.contains("socket") || msg.contains("eof") ->
                "Network error. Please check your connection and try again."

            msg.contains("invalid login credentials") || msg.contains("invalid credentials") ->
                "Incorrect email or password. Please try again."

            msg.contains("email not confirmed") ->
                "Please verify your email address before signing in."

            msg.contains("user already registered") || msg.contains("already registered") ->
                "An account with this email already exists. Please sign in instead."

            msg.contains("token has expired") || msg.contains("otp expired") ->
                "The verification code has expired. Please request a new one."

            msg.contains("invalid otp") || msg.contains("token is invalid") ->
                "Invalid verification code. Please check and try again."

            msg.contains("rate limit") || msg.contains("too many requests") ->
                "Too many attempts. Please wait a moment before trying again."

            msg.contains("jwt expired") || msg.contains("session expired") ->
                "Your session has expired. Please sign in again."

            msg.contains("not authenticated") || msg.contains("unauthorized") || msg.contains("401") ->
                "You need to sign in to do this."

            msg.contains("forbidden") || msg.contains("403") ->
                "You don't have permission to do this."

            msg.contains("not found") || msg.contains("404") ->
                "The requested item was not found."

            msg.contains("too large") || msg.contains("payload") ->
                "The file is too large. Please try a smaller file."

            msg.contains("duplicate") || msg.contains("unique violation") ->
                "This item already exists."

            msg.contains("password") && (msg.contains("weak") || msg.contains("short")) ->
                "Password must be at least 8 characters with a mix of letters and numbers."

            msg.contains("invalid email") ->
                "Please enter a valid email address."

            msg.contains("storage") && msg.contains("not found") ->
                "The file could not be found."

            msg.contains("supabase") || msg.contains("postgrest") || msg.contains("realtime") ||
            msg.contains("gotrue") || msg.contains("http") || msg.contains("url") ||
            msg.contains("exception") || msg.contains("error code") || msg.contains("status:") ->
                "Something went wrong. Please try again later."

            else -> raw.take(120).let {
                if (it.length < raw.length) "$it…" else it
            }
        }
    }

    fun isNetworkError(throwable: Throwable): Boolean {
        val msg = throwable.message?.lowercase() ?: ""
        return msg.contains("connection") || msg.contains("network") ||
               msg.contains("timeout") || msg.contains("socket") ||
               msg.contains("host") || msg.contains("eof")
    }
}
