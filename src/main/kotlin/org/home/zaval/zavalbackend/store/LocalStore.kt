package org.home.zaval.zavalbackend.store

import org.home.zaval.zavalbackend.model.Article
import org.home.zaval.zavalbackend.model.ArticleToTodoConnection
import org.home.zaval.zavalbackend.model.Todo
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class LocalStore : ObservableStore() {
    var todos by observedValue<List<Todo>>()
    var articles by observedValue<List<Article>>()
    var articlesToTodosConnections by observedValue<List<ArticleToTodoConnection>>()
}