package com.budgetbuddy.mobile.data

import com.budgetbuddy.mobile.model.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("auth/register")
    fun register(@Body request: RegisterRequest): Call<AuthResponse>

    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<AuthResponse>

    @GET("inbox")
    fun inbox(@Header("Authorization") authorization: String): Call<ApiResponse<List<InboxNotification>>>

    @POST("invitations/{invitationId}/accept")
    fun acceptInvitation(@Header("Authorization") authorization: String, @Path("invitationId") invitationId: Long): Call<ApiResponse<Invitation>>

    @POST("invitations/{invitationId}/decline")
    fun declineInvitation(@Header("Authorization") authorization: String, @Path("invitationId") invitationId: Long): Call<ApiResponse<Invitation>>

    @GET("groups")
    fun groups(@Header("Authorization") authorization: String): Call<ApiResponse<List<Group>>>

    @GET("groups/{groupId}/transactions")
    fun groupTransactions(@Header("Authorization") authorization: String, @Path("groupId") groupId: Long): Call<ApiResponse<List<GroupTransaction>>>

    @GET("groups/{groupId}/history")
    fun groupHistory(@Header("Authorization") authorization: String, @Path("groupId") groupId: Long): Call<ApiResponse<List<GroupHistoryItem>>>

    @POST("budgets")
    fun createBudget(@Header("Authorization") authorization: String, @Body request: BudgetRequest): Call<ApiResponse<Budget>>

    @GET("budgets/tracking")
    fun budgetTracking(@Header("Authorization") authorization: String): Call<ApiResponse<List<BudgetTracking>>>

    @POST("saving-goals")
    fun createSavingGoal(@Header("Authorization") authorization: String, @Body request: SavingGoalRequest): Call<ApiResponse<SavingGoal>>

    @GET("saving-goals")
    fun savingGoals(@Header("Authorization") authorization: String): Call<ApiResponse<List<SavingGoal>>>

    @GET("groups/{groupId}/budgets/tracking")
    fun groupBudgetTracking(@Header("Authorization") authorization: String, @Path("groupId") groupId: Long): Call<ApiResponse<List<BudgetTracking>>>

    @GET("groups/{groupId}/saving-goals")
    fun groupSavingGoals(@Header("Authorization") authorization: String, @Path("groupId") groupId: Long): Call<ApiResponse<List<SavingGoal>>>
}


