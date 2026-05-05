package com.dentical.staff.ui.navigation

import androidx.lifecycle.ViewModel
import com.dentical.staff.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NavSessionViewModel @Inject constructor(
    val sessionManager: SessionManager
) : ViewModel()
