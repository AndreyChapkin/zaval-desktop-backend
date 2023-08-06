package org.home.zaval.zavalbackend.entity.value

enum class TodoStatus(val priority: Int) {
    DONE(0),
    BACKLOG(1),
    WILL_BE_BACK(2),
    PING_ME(3),
    NEXT_TO_TAKE(4),
    IN_PROGRESS(5),
}