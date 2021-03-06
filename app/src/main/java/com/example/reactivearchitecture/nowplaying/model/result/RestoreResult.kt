/*
Copyright 2017 LEO LLC

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute,
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.example.reactivearchitecture.nowplaying.model.result

import com.example.reactivearchitecture.nowplaying.model.MovieInfo

/**
 * Results from [com.example.reactivearchitecture.nowplaying.model.action.RestoreAction] requests.
 * @param resultType -
 * @param isSuccessful -
 * @param isLoading -
 * @param pageNumber -
 * @param result -
 * @param error -
 */
class RestoreResult(
    @field:ResultType override val type: Int,
    val isSuccessful: Boolean,
    val isLoading: Boolean,
    val pageNumber: Int,
    val result: List<MovieInfo>?,
    val error: Throwable?
) : Result() {

    /**
     * Note - Kotlin CFM
     */
    companion object {

        fun inFlight(pageNumber: Int, result: List<MovieInfo>?): RestoreResult {
            return RestoreResult(
                    Result.IN_FLIGHT,
                    true,
                    true,
                    pageNumber,
                    result,
                    null
            )
        }

        fun success(pageNumber: Int, result: List<MovieInfo>): RestoreResult {
            return RestoreResult(
                    Result.SUCCESS,
                    true,
                    false,
                    pageNumber,
                    result,
                    null
            )
        }

        fun failure(pageNumber: Int, inProgress: Boolean, error: Throwable): RestoreResult {
            return RestoreResult(
                    Result.FAILURE,
                    false,
                    inProgress,
                    pageNumber,
                    null,
                    error)
        }
    }
}
