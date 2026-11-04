package gamemaster.gui;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.SwingWorker;

/** Runs a slow job (a model call) off the screen thread, then reports back on the screen thread. */
final class Background {

    private Background() {}

    static <T> void run(Callable<T> job, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return job.call();
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (ExecutionException e) {
                    onError.accept(e.getCause() == null ? e : e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    onError.accept(e);
                }
            }
        }.execute();
    }

    static String describe(Throwable t) {
        return t.getMessage() == null || t.getMessage().isBlank() ? t.toString() : t.getMessage();
    }
}
