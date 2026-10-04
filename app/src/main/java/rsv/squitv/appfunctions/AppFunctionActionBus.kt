package rsv.squitv.appfunctions

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppFunctionActionBus @Inject constructor() {
    sealed class Action {
        data class PlayMedia(
            val streamId: Int,
            val name: String,
            val type: String,
            val epgId: String? = null,
            val container: String? = null
        ) : Action()
        data object Pause : Action()
        data object Resume : Action()
        data object Stop : Action()
    }

    private val _actions = MutableSharedFlow<Action>()
    val actions = _actions.asSharedFlow()

    suspend fun emit(action: Action) {
        _actions.emit(action)
    }
}
