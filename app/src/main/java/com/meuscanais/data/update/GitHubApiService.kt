package com.meuscanais.data.update

import retrofit2.http.GET
import retrofit2.http.Path

interface GitHubApiService {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String = GitHubUpdateConfig.OWNER,
        @Path("repo") repo: String = GitHubUpdateConfig.REPO
    ): GitHubReleaseResponse
}
