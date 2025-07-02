
import androidx.compose.runtime.*
import arrow.core.some
import io.github.yafrl.compose.composeState
import io.github.yafrl.timeline.Timeline
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.Element

fun main() {
    Timeline.initializeTimeline()

    renderComposable(rootElementId = "root") {
        TodoApp()
    }
}

@Composable
fun TodoApp() {
    val viewModel = remember { TodoMvcViewModel() }

    // Collect StateFlows into Compose state
    val newText        by remember { viewModel.newTodoText.composeState() }
    val allTodos       by remember { viewModel.allTodos.composeState() }
    val todos          by remember { viewModel.todos.composeState() }
    val activeCount    by remember { viewModel.activeCount.composeState() }
    val completedCount by remember { viewModel.completedCount.composeState() }
    val filter         by remember { viewModel.currentFilter.composeState() }
    val editingId      by remember { viewModel.editingItemId.composeState() }
    val editText       by remember { viewModel.editingText.composeState() }

    // Root wrapper
    Div(attrs = { classes("todoapp") }) {
        // Header
        Header {
            H1 { Text("todos") }
            Input(type = InputType.Text) {
                classes("new-todo")
                placeholder("What needs to be done?")
                autoFocus()
                value(newText)
                onInput { viewModel.onNewTextChanged.send(it.value) }
                onKeyDown {
                    if (it.key == "Enter") viewModel.addTodo.send(Unit)
                }
            }
        }

        // Main section
        if (todos.isNotEmpty()) {
            Section(attrs = { classes("main") }) {
                Div(attrs = { classes("toggle-all-container")}) {
                    Input(type = InputType.Checkbox) {
                        id("toggle-all")
                        classes("toggle-all")
                        checked(activeCount == 0)
                        onClick { viewModel.toggleAll.send(Unit) }
                    }
                    Label(forId = "toggle-all") { Text("Toggle All Input") }
                }

                Ul(attrs = { classes("todo-list") }) {
                    todos.forEach { todo ->
                        val isEditing = (todo.id.some() == editingId)

                        if (isEditing) {
                            // Editor input
                            Input(type = InputType.Text) {
                                classes("edit")
                                value(editText)
                                autoFocus()
                                onInput { viewModel.updateEditingText.send(it.value) }
                                onKeyDown {
                                    when (it.key) {
                                        "Enter" -> viewModel.submitEdit.send(todo.id)
                                        "Escape" -> viewModel.cancelEdit.send(Unit)
                                    }
                                }
                                onBlur { viewModel.submitEdit.send(todo.id) }
                            }
                        } else {
                            // Normal view
                            Li(
                                attrs = { if (todo.completed) classes("completed") }
                            ) {
                                Div(attrs = { classes("view") }) {
                                    Input(type = InputType.Checkbox) {
                                        classes("toggle")
                                        checked(todo.completed)
                                        onClick { viewModel.toggleTodo.send(todo.id) }
                                    }
                                    Label(attrs = {
                                        // listen for dblclick to start editing
                                        onDoubleClick { viewModel.startEditing.send(todo.id) }
                                    }) {
                                        Text(todo.text)
                                    }
                                    Button(attrs = {
                                        classes("destroy")
                                        onClick { viewModel.deleteTodo.send(todo.id) }
                                    }) { }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Footer
        if (allTodos.isNotEmpty()) {
            Footer(attrs = { classes("footer") }) {
                Span(attrs = { classes("todo-count") }) {
                    TagElement<Element>(tagName = "strong", applyAttrs = null) {
                        Text("$activeCount")
                    }
                    Text(" items left")
                }
                Ul(attrs = { classes("filters") }) {
                    listOf(
                        TodoFilter.All to "#/",
                        TodoFilter.Active to "#/active",
                        TodoFilter.Completed to "#/completed"
                    ).forEach { (f, href) ->
                        Li(attrs = { if (filter == f) classes("selected") }) {
                            A(attrs = {
                                href(href)
                                onClick { viewModel.setFilter.send(f) }
                            }) {
                                Text(f.name)
                            }
                        }
                    }
                }
                if (completedCount > 0) {
                    Button(attrs = {
                        classes("clear-completed")
                        onClick { viewModel.clearCompleted.send(Unit) }
                    }) { Text("Clear completed") }
                }
            }
        }
    }

    // Info section
    Footer(attrs = {
        classes("info")
    }) {
        P { Text("Double-click to edit a todo") }
        P {
            Text("Written by ")
            A(attrs = { href("https://github.com/sintrastes") }) {
                Text("Nathan Bedell")
            }
            Text(" with ")
            A(attrs = { href("https://github.com/yafrl/yafrl")}) {
                Text("yafrl")
            }
            Text(" and ")
            A(attrs = { href("https://github.com/JetBrains/compose-multiplatform?tab=readme-ov-file#compose-html")}) {
                Text("Compose HTML")
            }
        }
        P {
            A(attrs = {
                href("https://todomvc.com")
            }) {
                Text("Part of TodoMVC")
            }
        }
    }
}
