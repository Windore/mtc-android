#[cfg(target_os = "android")]
#[allow(non_snake_case)]

use chrono::{NaiveDate, Weekday};
use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString};
use jni::sys::{jint, jlong, jlongArray, jsize, jstring};
use mtc::{Event, MtcItem, MtcList, sync_remote, Task, Todo};
use num_traits::cast::FromPrimitive;
use ssh2::Session;
use std::io::Error;
use std::net::TcpStream;
use std::path::Path;

// Doing this with a static is much easier than the alternatives that seem to be available.
static mut TODO_MTC_LIST: Option<MtcList<Todo>> = None;
static mut TASK_MTC_LIST: Option<MtcList<Task>> = None;
static mut EVENT_MTC_LIST: Option<MtcList<Event>> = None;

// This code assumes that most of the Java code is correct. For example null values should never be passed
// or ids should always be valid. Only syncing and loading MtcLists from json can fail from "invalid"
// arguments.

// In addition there is some unnecessary code repetition here but I am probably not going to do anything about
// that unless I'll have to make changes to it.

#[no_mangle]
pub unsafe extern "C" fn Java_com_github_windore_mtca_mtc_Mtc_nativeInit(_: JNIEnv, _: JClass) {
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

#[no_mangle]
pub unsafe extern "C" fn Java_com_github_windore_mtca_mtc_Mtc_nativeSync(
    env: JNIEnv,
    _: JClass,
    username_j: JString,
    address_j: JString,
    path_j: JString,
    password_j: JString,
) -> jstring {
    let username: String = env.get_string(username_j).unwrap().into();
    let address: String = env.get_string(address_j).unwrap().into();
    let path: String = env.get_string(path_j).unwrap().into();
    let password: String = env.get_string(password_j).unwrap().into();

    // Remove expired events. Todos or tasks cannot expire.
    EVENT_MTC_LIST.as_mut().unwrap().remove_expired();

    let sync_result = sync_inner(&username, &address, &path, &password);
    if sync_result.is_ok() {
        JObject::null().into_inner()
    } else {
        env.new_string(sync_result.err().unwrap().to_string())
            .unwrap()
            .into_inner()
    }
}

unsafe fn sync_inner(
    username: &str,
    address: &str,
    path: &str,
    password: &str,
) -> Result<(), Error> {
    let tcp = TcpStream::connect(address)?;
    let mut session = Session::new()?;
    session.set_tcp_stream(tcp);
    session.handshake()?;
    session.userauth_password(username, password)?;

    sync_remote(
        &session,
        TODO_MTC_LIST.as_mut().unwrap(),
        &Path::new(path).join(Path::new("todos.json")),
        false,
    )?;
    sync_remote(
        &session,
        TASK_MTC_LIST.as_mut().unwrap(),
        &Path::new(path).join(Path::new("tasks.json")),
        false,
    )?;
    sync_remote(
        &session,
        EVENT_MTC_LIST.as_mut().unwrap(),
        &Path::new(path).join(Path::new("events.json")),
        false,
    )?;

    Ok(())
}

pub mod todos {
    use super::*;

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
