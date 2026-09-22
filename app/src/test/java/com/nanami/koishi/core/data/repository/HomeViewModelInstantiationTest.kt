package com.nanami.koishi.core.data.repository

import android.app.Application
import com.nanami.koishi.feature.home.HomeViewModel
import org.junit.Assert.assertNotNull
import org.junit.Test

class HomeViewModelInstantiationTest {

    @Test
    fun testHomeViewModelHasApplicationConstructor() {
        // ViewModelProvider.AndroidViewModelFactory 通过反射寻找 (Application) 单参构造函数
        val constructor = HomeViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull(constructor)
    }
}
