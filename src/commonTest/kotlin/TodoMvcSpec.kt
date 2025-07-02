import io.github.yafrl.signal
import io.github.yafrl.testing.testPropositionHoldsFor
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.resolution.GlobalArbResolver
import kotlin.reflect.typeOf

import kotlin.test.Test

// Note: Kotest FunSpec not working for some reason. Have to use kotlin-test.
// Specification inspired by https://gist.github.com/owickstrom/1a0698ef6a47df07dfc1fe59eda12983
class TodoMvcSpec {
    // Setup custom arbitrary
    init {
        GlobalArbResolver.register(typeOf<TodoID>(), arbitrary {
            TodoID("todo-${Arb.int().bind()}")
        })

        GlobalArbResolver.register(typeOf<Unit>(), arbitrary {  })

        GlobalArbResolver.register(typeOf<TodoFilter>(), arbitrary<TodoFilter> {
            val filter = Arb.int(0, 2).bind()
            when (filter) {
                0 -> TodoFilter.All
                1 -> TodoFilter.Active
                else -> TodoFilter.Completed
            }
        })
    }

    @Test
    fun `TodoMVC Specification`() = testPropositionHoldsFor(
        setupState = {
            val viewModel = TodoMvcViewModel()

            signal {
                val todos = viewModel.todos.bind()

                TodoState(
                    todos.map { it.text },
                    viewModel.newTodoText.bind(),
                    viewModel.currentFilter.bind(),
                    viewModel.activeCount.bind(),
                    todos.filter { !it.completed }.size,
                    todos.filter { it.completed }.size
                )
            }
        },
        proposition = {
            val initial by condition {
                current.itemTexts.isEmpty()
            }

            val enterText by condition {
                current.itemTexts == next.itemTexts &&
                        current.pendingText != next.pendingText &&
                        current.selectedFilter == next.selectedFilter
            }

            val addNew by condition {
                next.pendingText == "" && when(next.selectedFilter) {
                    TodoFilter.All -> current.pendingText == next.itemTexts.lastOrNull()
                    TodoFilter.Active -> current.pendingText == next.itemTexts.lastOrNull()
                    TodoFilter.Completed -> current.itemTexts == next.itemTexts
                }
            }

            val changeFilter by condition {
                when {
                    current.selectedFilter == TodoFilter.All ->
                        current.numItems >= next.numItems
                    next.selectedFilter == TodoFilter.Active ->
                        next.numItemsLeft == current.numUnchecked &&
                                current.numItems == current.numUnchecked
                    current.selectedFilter == next.selectedFilter -> false
                    else -> current.pendingText == next.pendingText
                }
            }

            val checkOne by condition {
                current.pendingText == next.pendingText &&
                        current.selectedFilter == next.selectedFilter &&
                        (current.selectedFilter != TodoFilter.Completed) &&
                        ((current.selectedFilter == TodoFilter.All)
                            implies (current.numItems == next.numItems && current.numChecked < next.numChecked)
                        ) &&
                        ((current.selectedFilter == TodoFilter.Active)
                            implies (current.numItems > next.numItems && current.numItemsLeft > next.numItemsLeft)
                        )
            }

            val uncheckOne by condition {
                current.pendingText == next.pendingText &&
                        current.selectedFilter == next.selectedFilter &&
                        (current.selectedFilter != TodoFilter.Active) &&
                        ((current.selectedFilter == TodoFilter.All)
                          implies (current.numItems == next.numItems && current.numChecked > next.numChecked)
                        ) &&
                        ((current.selectedFilter == TodoFilter.Completed)
                          implies (current.numItems > next.numItems && current.numItemsLeft < next.numItemsLeft)
                        )
            }

            val delete by condition {
                current.pendingText == next.pendingText && if (current.numItems == 1) {
                    next.numItems == 0
                } else {
                    current.selectedFilter == next.selectedFilter &&
                            next.numItems == current.numItems - 1 && when(current.selectedFilter) {
                                TodoFilter.All -> true
                                TodoFilter.Active -> current.numItemsLeft == (next.numItemsLeft - 1)
                                TodoFilter.Completed -> current.numItemsLeft == next.numItemsLeft
                            }
                }
            }

            val toggleAll by condition {
                current.pendingText == next.pendingText &&
                        current.selectedFilter == next.selectedFilter &&
                        when(current.selectedFilter) {
                            TodoFilter.All -> current.numItems == next.numItems && next.numItems == next.numChecked
                            TodoFilter.Active -> (current.numItems > 0) implies (next.numItems == 0)
                                || (current.numItems == 0) implies (next.numItems > 0)
                            TodoFilter.Completed -> current.numItems + current.numItemsLeft == next.numItems
                        }
            }

            val hasFilters by condition {
                true
            }

            // Initial condition
            initial and always(
                // State transitions
                enterText or
                    addNew or
                    changeFilter or
                    checkOne or
                    uncheckOne or
                    delete or
                    toggleAll
            ) and always(
                // Global invariants
                hasFilters
            )
        }
    )

    data class TodoState(
        val itemTexts: List<String>,
        val pendingText: String,
        val selectedFilter: TodoFilter,
        val numItemsLeft: Int,
        val numUnchecked: Int,
        val numChecked: Int
    )

    val TodoState.numItems get() = itemTexts.size
}

infix fun Boolean.implies(other: Boolean) = !this || other