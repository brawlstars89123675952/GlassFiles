#include <jni.h>
#include <stdlib.h>
#include <unistd.h>
#include <pty.h>
#include <fcntl.h>
#include <errno.h>
#include <string.h>
#include <signal.h>
#include <sys/ioctl.h>
#include <sys/wait.h>

static char *jstr_to_c(JNIEnv *env, jstring s) {
    if (!s) return NULL;
    const char *u = (*env)->GetStringUTFChars(env, s, NULL);
    char *c = strdup(u);
    (*env)->ReleaseStringUTFChars(env, s, u);
    return c;
}

JNIEXPORT jintArray JNICALL
Java_com_glassfiles_data_terminal_PtyNative_nativeCreate(
    JNIEnv *env, jclass clz,
    jstring jCmd, jobjectArray jArgs, jobjectArray jEnv,
    jint rows, jint cols)
{
    char *cmd = jstr_to_c(env, jCmd);
    if (!cmd) return NULL;

    int argc = jArgs ? (*env)->GetArrayLength(env, jArgs) : 0;
    char **argv = calloc(argc + 2, sizeof(char *));
    argv[0] = strdup(cmd);
    for (int i = 0; i < argc; i++) {
        argv[i + 1] = jstr_to_c(env, (*env)->GetObjectArrayElement(env, jArgs, i));
    }
    argv[argc + 1] = NULL;

    int envc = jEnv ? (*env)->GetArrayLength(env, jEnv) : 0;
    char **envp = calloc(envc + 1, sizeof(char *));
    for (int i = 0; i < envc; i++) {
        envp[i] = jstr_to_c(env, (*env)->GetObjectArrayElement(env, jEnv, i));
    }
    envp[envc] = NULL;

    struct winsize ws = { .ws_row = rows, .ws_col = cols, .ws_xpixel = 0, .ws_ypixel = 0 };
    int master;
    pid_t pid = forkpty(&master, NULL, NULL, &ws);

    if (pid < 0) {
        free(cmd);
        for (int i = 0; argv[i]; i++) free(argv[i]);
        free(argv);
        for (int i = 0; envp[i]; i++) free(envp[i]);
        free(envp);
        return NULL;
    }

    if (pid == 0) {
        /* Child: set env then exec */
        setsid();
        for (int i = 0; envp[i]; i++) putenv(envp[i]);
        execvp(cmd, argv);
        _exit(127);
    }

    /* Parent */
    free(cmd);
    for (int i = 0; argv[i]; i++) free(argv[i]);
    free(argv);
    for (int i = 0; envp[i]; i++) free(envp[i]);
    free(envp);

    jintArray res = (*env)->NewIntArray(env, 2);
    jint buf[2] = { master, (jint)pid };
    (*env)->SetIntArrayRegion(env, res, 0, 2, buf);
    return res;
}

JNIEXPORT jint JNICALL
Java_com_glassfiles_data_terminal_PtyNative_nativeRead(
    JNIEnv *env, jclass clz, jint fd, jbyteArray jBuf)
{
    int len = (*env)->GetArrayLength(env, jBuf);
    jbyte *buf = (*env)->GetByteArrayElements(env, jBuf, NULL);
    int n = read(fd, buf, len);
    (*env)->ReleaseByteArrayElements(env, jBuf, buf, 0);
    return n;
}

JNIEXPORT jint JNICALL
Java_com_glassfiles_data_terminal_PtyNative_nativeWrite(
    JNIEnv *env, jclass clz, jint fd, jbyteArray jData, jint len)
{
    jbyte *data = (*env)->GetByteArrayElements(env, jData, NULL);
    int n = write(fd, data, len);
    (*env)->ReleaseByteArrayElements(env, jData, data, JNI_ABORT);
    return n;
}

JNIEXPORT void JNICALL
Java_com_glassfiles_data_terminal_PtyNative_nativeResize(
    JNIEnv *env, jclass clz, jint fd, jint rows, jint cols)
{
    struct winsize ws = { .ws_row = rows, .ws_col = cols };
    ioctl(fd, TIOCSWINSZ, &ws);
}

JNIEXPORT void JNICALL
Java_com_glassfiles_data_terminal_PtyNative_nativeClose(
    JNIEnv *env, jclass clz, jint fd)
{
    close(fd);
}

JNIEXPORT jint JNICALL
Java_com_glassfiles_data_terminal_PtyNative_nativeWaitFor(
    JNIEnv *env, jclass clz, jint pid)
{
    int status;
    waitpid(pid, &status, 0);
    return WIFEXITED(status) ? WEXITSTATUS(status) : -1;
}
