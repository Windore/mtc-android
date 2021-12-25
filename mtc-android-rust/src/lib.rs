#[cfg(target_os = "android")]
#[allow(non_snake_case)]
pub mod android {
    use jni::JNIEnv;
    use jni::objects::JClass;
    use jni::sys::jstring;
    use std::ffi::CString;

    #[no_mangle]
    pub unsafe extern fn Java_com_github_windore_mtca_MtcApplication_test(env: JNIEnv, _: JClass) -> jstring {
        let world_ptr = CString::new("Hello world from Rust world").unwrap();
        let output = env.new_string(world_ptr.to_str().unwrap()).expect("Couldn't create java string!");
        output.into_inner()
    }
}