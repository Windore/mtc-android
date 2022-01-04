#[cfg(target_os = "android")]
#[allow(non_snake_case)]
use chrono::{NaiveDate, Weekday};
use jni::objects::{JClass, JString};
use jni::sys::{jint, jlong, jlongArray, jsize, jstring};
use jni::JNIEnv;
use mtc::{Event, MtcItem, MtcList, Task, Todo};
use num_traits::cast::FromPrimitive;

// Doing this with a static is much easier than the alternatives that seem to be available.
static mut TODO_MTC_LIST: Option<MtcList<Todo>> = None;
static mut TASK_MTC_LIST: Option<MtcList<Task>> = None;
static mut EVENT_MTC_LIST: Option<MtcList<Event>> = None;

// This code very much expects that Java code is correct.
// For example all ids should be valid and items of those ids are not marked as removed.
// Except for nativeInitSaved which accepts invalid json.

// In addition some unwrap method calls probably aren't that safe but for now I'm not going to do
// much about it.

// TODO look into options of logging errors etc from rust

#[no_mangle]
pub unsafe extern "C" fn Java_com_github_windore_mtca_mtc_Mtc_nativeInit(
    _: JNIEnv,
    _: JClass,
) {
    TODO_MTC_LIST = Some(MtcList::new(false));
    TASK_MTC_LIST = Some(MtcList::new(false));
    EVENT_MTC_LIST = Some(MtcList::new(false));
}

#[no_mangle]
pub unsafe extern "C" fn Java_com_github_windore_mtca_mtc_Mtc_nativeInitSaved(
    env: JNIEnv,
    _: JClass,
    todo_json: JString,
    task_json: JString,
    event_json: JString,
) {
    let todo: String = env.get_string(todo_json).unwrap().into();
    let todos = serde_json::from_str(&todo);
    if todos.is_ok() {
        TODO_MTC_LIST = Some(todos.unwrap());
    } else {
        TODO_MTC_LIST = Some(MtcList::new(false));
    }

    let task: String = env.get_string(task_json).unwrap().into();
    let tasks = serde_json::from_str(&task);
    if tasks.is_ok() {
        TASK_MTC_LIST = Some(tasks.unwrap());
    } else {
        TASK_MTC_LIST = Some(MtcList::new(false));
    }

    let event: String = env.get_string(event_json).unwrap().into();
    let events = serde_json::from_str(&event);
    if events.is_ok() {
        EVENT_MTC_LIST = Some(events.unwrap());
    } else {
        EVENT_MTC_LIST = Some(MtcList::new(false));
    }
}

pub mod todos {
    use super::*;
    use jni::objects::JObject;

    #[no_mangle]
    pub unsafe extern "C" fn Java_com_github_windore_mtca_mtc_Mtc_nativeAddTodo(
        env: JNIEnv,
        _: JClass,
        body: JString,
        weekday_n: jint,
    ) -> jlong {
        let day = Weekday::from_i32(weekday_n);
        let body = env.get_string(body).unwrap().into();

        TODO_MTC_LIST.as_mut().unwrap().add(Todo::new(body, day)) as i64
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_com_github_windore_mtca_mtc_Mtc_nativeGetTodoString(
        env: JNIEnv,
        _: JClass,
        id: jlong,
    ) -> jstring {
        let item = TODO_MTC_LIST.as_ref().unwrap().get_by_id(id as usize);
        let body = item.unwrap().to_string();
        env.new_string(body).unwrap().into_inner()
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_com_github_windore_mtca_mtc_Mtc_nativeRemoveTodo(
        _: JNIEnv,
        _: JClass,
        id: jlong,
    ) {
        TODO_MTC_LIST
            .as_mut()
            .unwrap()
            .mark_removed(id as usize)
            .unwrap();
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_com_github_windore_mtca_mtc_Mtc_nativeGetTodos(
        env: JNIEnv,
        _: JClass,
    ) -> jlongArray {
        let ids: Vec<jlong> = TODO_MTC_LIST
            .as_ref()
            .unwrap()
            .items()
            .iter()
            .map(|item| item.id() as jlong)
            .collect();
        let long_array = env.new_long_array(ids.len() as jsize).unwrap();
        env.set_long_array_region(long_array, 0, &ids).unwrap();
        long_array
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_com_github_windore_mtca_mtc_Mtc_nativeGetTodosForDate(
        env: JNIEnv,
        _: JClass,
        year: jint,
        month: jint,
        day: jint,
    ) -> jlongArray {
        let date = NaiveDate::from_ymd(year, month as u32, day as u32);

        let ids: Vec<jlong> = TODO_MTC_LIST
            .as_ref()
            .unwrap()
            .items_for_date(date)
            .iter()
            .map(|item| item.id() as jlong)
            .collect();
        let long_array = env.new_long_array(ids.len() as jsize).unwrap();
        env.set_long_array_region(long_array, 0, &ids).unwrap();
        long_array
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_com_github_windore_mtca_mtc_Mtc_nativeGetTodosForWeekday(
        env: JNIEnv,
        _: JClass,
        weekday_n: jint,
    ) -> jlongArray {
        let weekday = Weekday::from_i32(weekday_n).unwrap();
        let ids: Vec<jlong> = TODO_MTC_LIST
            .as_ref()
            .unwrap()
            .items_for_weekday(weekday)
            .iter()
            .map(|item| item.id() as jlong)
            .collect();
        let long_array = env.new_long_array(ids.len() as jsize).unwrap();
        env.set_long_array_region(long_array, 0, &ids).unwrap();
        long_array
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_com_github_windore_mtca_mtc_Mtc_nativeGetTodoJsonString(
        env: JNIEnv,
        _: JClass,
    ) -> jstring {
        let json = serde_json::to_string(TODO_MTC_LIST.as_ref().unwrap()).unwrap();
        env.new_string(json).unwrap().into_inner()
    }
}

pub mod tasks {
    use super::*;

    #[no_mangle]
    pub unsafe extern "C" fn Java_com_github_windore_mtca_mtc_Mtc_nativeAddTask(
        env: JNIEnv,
        _: JClass,
        body: JString,
        weekday_n: jint,
        duration: jlong,
    ) -> jlong {
        let day = Weekday::from_i32(weekday_n);
        let body = env.get_string(body).unwrap().into();

        TASK_MTC_LIST
            .as_mut()
            .unwrap()
            .add(Task::new(body, duration as u32, day)) as i64
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_com_github_windore_mtca_mtc_Mtc_nativeGetTaskString(
        env: JNIEnv,
        _: JClass,
        id: jlong,
    ) -> jstring {
        let item = TASK_MTC_LIST.as_ref().unwrap().get_by_id(id as usize);
        let body = item.unwrap().to_string();
        env.new_string(body).unwrap().into_inner()
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_com_github_windore_mtca_mtc_Mtc_nativeGetTaskDuration(
        _: JNIEnv,
        _: JClass,
        id: jlong,
    ) -> jlong {
        let item = TASK_MTC_LIST.as_ref().unwrap().get_by_id(id as usize);
        item.unwrap().duration() as i64
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_com_github_windore_mtca_mtc_Mtc_nativeRemoveTask(
        _: JNIEnv,
        _: JClass,
        id: jlong,
    ) {
        TASK_MTC_LIST
            .as_mut()
            .unwrap()
            .mark_removed(id as usize)
            .unwrap();
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_com_github_windore_mtca_mtc_Mtc_nativeGetTasks(
        env: JNIEnv,
        _: JClass,
    ) -> jlongArray {
        let ids: Vec<jlong> = TASK_MTC_LIST
            .as_ref()
            .unwrap()
            .items()
            .iter()
            .map(|item| item.id() as jlong)
            .collect();
        let long_array = env.new_long_array(ids.len() as jsize).unwrap();
        env.set_long_array_region(long_array, 0, &ids).unwrap();
        long_array
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_com_github_windore_mtca_mtc_Mtc_nativeGetTasksForDate(
        env: JNIEnv,
        _: JClass,
        year: jint,
        month: jint,
        day: jint,
    ) -> jlongArray {
        let date = NaiveDate::from_ymd(year, month as u32, day as u32);
        let ids: Vec<jlong> = TASK_MTC_LIST
            .as_ref()
            .unwrap()
            .items_for_date(date)
            .iter()
            .map(|item| item.id() as jlong)
            .collect();
        let long_array = env.new_long_array(ids.len() as jsize).unwrap();
        env.set_long_array_region(long_array, 0, &ids).unwrap();
        long_array
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_com_github_windore_mtca_mtc_Mtc_nativeGetTasksForWeekday(
        env: JNIEnv,
        _: JClass,
        weekday_n: jint,
    ) -> jlongArray {
        let weekday = Weekday::from_i32(weekday_n).unwrap();
        let ids: Vec<jlong> = TASK_MTC_LIST
            .as_ref()
            .unwrap()
            .items_for_weekday(weekday)
            .iter()
            .map(|item| item.id() as jlong)
            .collect();
        let long_array = env.new_long_array(ids.len() as jsize).unwrap();
        env.set_long_array_region(long_array, 0, &ids).unwrap();
        long_array
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_com_github_windore_mtca_mtc_Mtc_nativeGetTaskJsonString(
        env: JNIEnv,
        _: JClass,
    ) -> jstring {
        let json = serde_json::to_string(TASK_MTC_LIST.as_ref().unwrap()).unwrap();
        env.new_string(json).unwrap().into_inner()
    }
}

pub mod events {
    use super::*;

    #[no_mangle]
    pub unsafe extern "C" fn Java_com_github_windore_mtca_mtc_Mtc_nativeAddEvent(
        env: JNIEnv,
        _: JClass,
        body: JString,
        year: jint,
        month: jint,
        day: jint,
    ) -> jlong {
        let date = NaiveDate::from_ymd(year, month as u32, day as u32);
        let body = env.get_string(body).unwrap().into();

        EVENT_MTC_LIST.as_mut().unwrap().add(Event::new(body, date)) as i64
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_com_github_windore_mtca_mtc_Mtc_nativeGetEventString(
        env: JNIEnv,
        _: JClass,
        id: jlong,
    ) -> jstring {
        let item = EVENT_MTC_LIST.as_ref().unwrap().get_by_id(id as usize);
        let body = item.unwrap().to_string();
        env.new_string(body).unwrap().into_inner()
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_com_github_windore_mtca_mtc_Mtc_nativeRemoveEvent(
        _: JNIEnv,
        _: JClass,
        id: jlong,
    ) {
        EVENT_MTC_LIST
            .as_mut()
            .unwrap()
            .mark_removed(id as usize)
            .unwrap();
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_com_github_windore_mtca_mtc_Mtc_nativeGetEvents(
        env: JNIEnv,
        _: JClass,
    ) -> jlongArray {
        let ids: Vec<jlong> = EVENT_MTC_LIST
            .as_ref()
            .unwrap()
            .items()
            .iter()
            .map(|item| item.id() as jlong)
            .collect();
        let long_array = env.new_long_array(ids.len() as jsize).unwrap();
        env.set_long_array_region(long_array, 0, &ids).unwrap();
        long_array
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_com_github_windore_mtca_mtc_Mtc_nativeGetEventsForDate(
        env: JNIEnv,
        _: JClass,
        year: jint,
        month: jint,
        day: jint,
    ) -> jlongArray {
        let date = NaiveDate::from_ymd(year, month as u32, day as u32);
        let ids: Vec<jlong> = EVENT_MTC_LIST
            .as_ref()
            .unwrap()
            .items_for_date(date)
            .iter()
            .map(|item| item.id() as jlong)
            .collect();
        let long_array = env.new_long_array(ids.len() as jsize).unwrap();
        env.set_long_array_region(long_array, 0, &ids).unwrap();
        long_array
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_com_github_windore_mtca_mtc_Mtc_nativeGetEventsForWeekday(
        env: JNIEnv,
        _: JClass,
        weekday_n: jint,
    ) -> jlongArray {
        let weekday = Weekday::from_i32(weekday_n).unwrap();
        let ids: Vec<jlong> = EVENT_MTC_LIST
            .as_ref()
            .unwrap()
            .items_for_weekday(weekday)
            .iter()
            .map(|item| item.id() as jlong)
            .collect();
        let long_array = env.new_long_array(ids.len() as jsize).unwrap();
        env.set_long_array_region(long_array, 0, &ids).unwrap();
        long_array
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_com_github_windore_mtca_mtc_Mtc_nativeGetEventJsonString(
        env: JNIEnv,
        _: JClass,
    ) -> jstring {
        let json = serde_json::to_string(EVENT_MTC_LIST.as_ref().unwrap()).unwrap();
        env.new_string(json).unwrap().into_inner()
    }
}
