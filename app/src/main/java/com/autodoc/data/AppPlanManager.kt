package com.autodoc.data

import android.content.Context

class AppPlanManager(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun isProPlan(): Boolean {
        return preferences.getBoolean(KEY_IS_PRO_PLAN, false)
    }

    fun setProPlan(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_IS_PRO_PLAN, enabled)
            .apply()
    }

    fun getCurrentPlanName(): String {
        return if (isProPlan()) PLAN_PRO else PLAN_FREE
    }

    fun getFreePlanMaxCars(): Int {
        return FREE_PLAN_MAX_CARS
    }

    companion object {
        const val PLAN_FREE = "Free"
        const val PLAN_PRO = "Pro"
        const val FREE_PLAN_MAX_CARS = 3

        private const val PREFS_NAME = "autodoc_plan_preferences"
        private const val KEY_IS_PRO_PLAN = "is_pro_plan"
    }
}