package mtkdex.core.build.ssmen.utils.dnsUtil;

import org.minidns.hla.ResolverApi;
import org.minidns.hla.ResolverResult;
import org.minidns.record.Data;
import org.minidns.record.Record;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Set;

public class AsyncTask extends android.os.AsyncTask<String, Void, String> {

    private final WeakReference<Response> delegate;

    public AsyncTask(Response delegate) {
        this.delegate = new WeakReference<>(delegate);
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            String domain = params[0];
            String recordType = params[1];
            ResolverResult<? extends Data> result;
            try {
                Class<Data> dataClass = Record.TYPE.valueOf(recordType).getDataClass();
                if (dataClass == null) {
                    return "Record type " + recordType + " not supported";
                }

                result = ResolverApi.INSTANCE.resolve(domain, dataClass);
            } catch (IOException e) {
                return "Error performing lookup on type " + recordType + ": " + e.getMessage();
            }

            if (!result.wasSuccessful()) {
                return "Lookup of type " + recordType + " failed with response code " + result.getResponseCode();
            }

            Set<? extends Data> answers = result.getAnswers();
            if (answers.isEmpty()) {
                return "No records found for type " + recordType;
            }

            StringBuilder out = new StringBuilder();
            for (Data answer : answers) {
                out.append(answer.toString()).append("\n\n");
            }

            return out.toString();

        }catch (Exception ignored){
            return null;
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (result!=null){
            try {
                Response activity = delegate.get();
                if (activity != null) {
                    activity.processFinish(result);
                }
            }catch (Exception ignored){}
        }
    }
}
