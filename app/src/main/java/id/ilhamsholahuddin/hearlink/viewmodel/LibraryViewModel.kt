package id.ilhamsholahuddin.hearlink.viewmodel

import androidx.lifecycle.ViewModel
import id.ilhamsholahuddin.hearlink.data.SignWord
import id.ilhamsholahuddin.hearlink.data.signDictionary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine

class LibraryViewModel : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    val filteredWords = combine(_searchQuery, _selectedCategory) { query, category ->
        var list = signDictionary
        if (category != null) {
            list = list.filter { it.category == category }
        }
        if (query.isNotBlank()) {
            list = list.filter { it.title.contains(query, ignoreCase = true) }
        }
        list
    }

    val categories = signDictionary.map { it.category }.distinct()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }
    
    fun getWordById(id: Int): SignWord? {
        return signDictionary.find { it.id == id }
    }
}
