package org.home.zaval.zavalbackend.model.value

enum class TodoStatus(val priority: Int) {
    DONE(0),
    BACKLOG(1),
    WILL_BE_BACK(2),
    PING_ME(3),
    IN_PROGRESS(4),
}