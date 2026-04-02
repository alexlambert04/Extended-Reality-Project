package be.kuleuven.gt.extendedrealityproject.ar;

import android.content.Context;

import com.google.ar.core.Session;

public class ArSessionController {

    private Session session;

    public void initialize(Context context) throws Exception {
        if (session == null) {
            session = new Session(context);
        }
    }

    public Session getSession() {
        return session;
    }

    public void dispose() {
        if (session != null) {
            session.close();
            session = null;
        }
    }
}

