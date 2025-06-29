
import arrow.core.Option
import arrow.core.none
import arrow.core.some
import io.github.yafrl.Signal
import io.github.yafrl.externalEvent
import io.github.yafrl.on

value class TodoID(val text: String)
data class TodoItem(val id: TodoID, val text: String, val completed: Boolean)
enum class TodoFilter { All, Active, Completed }

class TodoMvcViewModel() {
    // -- External events --

    val addTodo = externalEvent<Unit>("add_todo")
    val onNewTextChanged = externalEvent<String>("new_text_changed")
    val toggleTodo = externalEvent<TodoID>("toggle_todo")
    val deleteTodo = externalEvent<TodoID>("delete_todo")
    val toggleAll = externalEvent<Unit>("toggle_all")
    val clearCompleted = externalEvent<Unit>("clear_completed")
    val setFilter = externalEvent<TodoFilter>()
    val startEditing = externalEvent<TodoID>("start_editing")
    val updateEditingText = externalEvent<String>("update_editing_text")
    val submitEdit = externalEvent<TodoID>("submit_edit")
    val cancelEdit = externalEvent<Unit>("cancel_edit")

    // -- UID generation --
    var latestUID = -1

    fun newUID(): TodoID {
        latestUID++
        return TodoID("todo-${latestUID}")
    }

    // -- ViewModel implementation --

    val allTodos: Signal<List<TodoItem>> = Signal.fold(
        initial = listOf<TodoItem>(),
        on(addTodo) { todos, _ ->
            val currentText = newTodoText.currentValue()

            if (currentText.isEmpty()) return@on todos

            val newItem = TodoItem(
                id        = newUID(),
                text      = currentText,
                completed = false
            )

            todos + listOf(newItem)
        },
        on(toggleAll) { todos, _ ->
            val allCompleted = todos.all { it.completed }
            todos.map { it.copy(completed = !allCompleted) }
        },
        on(toggleTodo) { todos, toggled ->
            todos.map { item ->
                if (item.id == toggled) item.copy(completed = !item.completed) else item
            }
        },
        on(deleteTodo) { todos, deleted -> todos.filterNot { it.id == deleted } },
        on(clearCompleted) { todos, _ -> todos.filterNot { it.completed} },
        on(submitEdit) { todos, id ->
            todos.map { item ->
                if (item.id == id) {
                    item.copy(text = editingText.currentValue())
                } else {
                    item
                }
            }
        }
    )

    val newTodoText: Signal<String> = Signal.fold("",
        on(onNewTextChanged) { newText, _ -> newText },
        on(addTodo) { _, _ -> "" }
    )

    private val completedTodos = allTodos.map { todos -> todos.filter { it.completed } }

    private val activeTodos = allTodos.map { todos -> todos.filter { !it.completed } }

    val currentFilter: Signal<TodoFilter> = Signal.hold(TodoFilter.All, setFilter)

    val todos = currentFilter.flatMap { filter ->
        when (filter) {
            TodoFilter.All -> allTodos
            TodoFilter.Active -> activeTodos
            TodoFilter.Completed -> completedTodos
        }
    }

    val editingItemId: Signal<Option<TodoID>> = Signal.fold(none(),
        on(startEditing) { _, id -> id.some() },
        on(cancelEdit) { _, _ -> none() },
        on(submitEdit) { _, _ -> none() }
    )

    val editingText: Signal<String>  = Signal.fold("",
        on(updateEditingText) { _, newText -> newText },
        on(startEditing) { _, id ->
            val todoItem = allTodos.currentValue().first { it.id == id }

            todoItem.text
        },
        on(cancelEdit) { _, _ -> "" },
        on(submitEdit) { _, _ -> "" }
    )

    // Derived counts
    val activeCount: Signal<Int> = allTodos
        .map { list -> list.count { !it.completed } }

    val completedCount: Signal<Int> = allTodos
        .map { list -> list.count { it.completed } }
}
