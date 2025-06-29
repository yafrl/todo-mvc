
import arrow.core.Option
import arrow.core.none
import arrow.core.some
import io.github.yafrl.Event
import io.github.yafrl.Signal
import io.github.yafrl.externalEvent
import io.github.yafrl.on

value class TodoID(val text: String)
data class TodoItem(val id: TodoID, val text: String, val completed: Boolean)
enum class TodoFilter { All, Active, Completed }

class TodoMvcViewModel() {
    // -- UID generation --
    var latestUID = -1

    fun newUID(): TodoID {
        latestUID++
        return TodoID("todo-${latestUID}")
    }

    // -- External events --

    private val addTodo = externalEvent<Unit>("add_todo")
    private val onNewTextChanged = externalEvent<String>("new_text_changed")
    private val toggleTodo = externalEvent<TodoID>("toggle_todo")
    private val deleteTodo = externalEvent<TodoID>("delete_todo")
    private val toggleAll = externalEvent<Unit>("toggle_all")
    private val clearCompleted = externalEvent<Unit>("clear_completed")
    private val setFilter = externalEvent<TodoFilter>()
    private val startEditing = externalEvent<TodoID>("start_editing")
    private val updateEditingText = externalEvent<String>("update_editing_text")
    private val submitEdit = externalEvent<TodoID>("submit_edit")
    private val cancelEdit = externalEvent<Unit>("cancel_edit")

    fun onNewTextChanged(text: String) = onNewTextChanged.send(text)
    fun addTodo() = addTodo.send(Unit)
    fun toggleTodo(id: TodoID) = toggleTodo.send(id)
    fun deleteTodo(id: TodoID) = deleteTodo.send(id)
    fun toggleAll() = toggleAll.send(Unit)
    fun clearCompleted() = clearCompleted.send(Unit)
    fun setFilter(filter: TodoFilter) = setFilter.send(filter)
    fun startEditing(id: TodoID) = startEditing.send(id)
    fun updateEditingText(text: String) = updateEditingText.send(text)
    fun submitEdit(id: TodoID) = submitEdit.send(id)
    fun cancelEdit() = cancelEdit.send(Unit)

    // TODO: This is currently order-dependent, and must be defined before newTodoText
    //  I think this dependency has to do with the order that child nodes are processed
    //  in the frame, and how that interacts with sampling behaviors.
    //  We may need to add some code to make sure consistency in ensured (i.e. only the value
    //  used at the beginning of the frame is used for behaviors).
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

    private val todoTextActions = Event.merged(
        onNewTextChanged.map { newText: String -> { _: String -> newText } },
        addTodo.map { { "" } }
    )

    val newTodoText: Signal<String> = Signal.fold("", todoTextActions) { text, action -> action(text) }

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

    // Exposed StateFlows

    val editingItemId: Signal<Option<TodoID>> = Signal.fold(none(),
        on(startEditing) { _, id -> id.some() },
        // TODO: Maybe add a binary merge operator
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
