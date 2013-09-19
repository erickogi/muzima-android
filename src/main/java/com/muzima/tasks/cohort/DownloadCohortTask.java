package com.muzima.tasks.cohort;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Cohort;
import com.muzima.controller.CohortController;
import com.muzima.tasks.DownloadMuzimaTask;
import com.muzima.utils.Constants;
import com.muzima.view.cohort.AllCohortsListFragment;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.muzima.utils.Constants.COHORT_PREFIX_PREF;
import static com.muzima.utils.Constants.COHORT_PREFIX_PREF_KEY;

public class DownloadCohortTask extends DownloadMuzimaTask {
    private static final String TAG = "DownloadCohortTask";

    public DownloadCohortTask(MuzimaApplication applicationContext) {
        super(applicationContext);
    }

    @Override
    protected Integer[] performTask(String[]... values){
        Integer[] result = new Integer[2];

        MuzimaApplication muzimaApplicationContext = getMuzimaApplicationContext();

        if (muzimaApplicationContext == null) {
            result[0] = CANCELLED;
            return result;
        }

        CohortController cohortController = muzimaApplicationContext.getCohortController();

        List<String> cohortPrefixes = getCohortPrefixes(muzimaApplicationContext);

        try {
            List<Cohort> cohorts;
            if (cohortPrefixes.isEmpty()) {
                cohorts = cohortController.downloadAllCohorts();
            } else {
                cohorts = cohortController.downloadCohortsByPrefix(cohortPrefixes);
            }

            if (checkIfTaskIsCancelled(result)) return result;

            cohortController.deleteAllCohorts();

            cohortController.saveAllCohorts(cohorts);

            result[0] = SUCCESS;
            result[1] = cohorts.size();

        } catch (CohortController.CohortDownloadException e) {
            Log.e(TAG, "Exception when trying to download cohorts");
            result[0] = DOWNLOAD_ERROR;
            return result;
        } catch (CohortController.CohortSaveException e) {
            Log.e(TAG, "Exception when trying to save cohorts");
            result[0] = SAVE_ERROR;
            return result;
        } catch (CohortController.CohortDeleteException e) {
            Log.e(TAG, "Exception when trying to delete cohorts");
            result[0] = DELETE_ERROR;
            return result;
        }
        return result;
    }

    private List<String> getCohortPrefixes(MuzimaApplication muzimaApplicationContext) {
        SharedPreferences cohortSharedPref = muzimaApplicationContext.getSharedPreferences(COHORT_PREFIX_PREF, Context.MODE_PRIVATE);
        Set<String> prefixes = cohortSharedPref.getStringSet(COHORT_PREFIX_PREF_KEY, new HashSet<String>());
        return new ArrayList<String>(prefixes);
    }

    @Override
    protected void onPostExecute(Integer[] result) {
        MuzimaApplication muzimaApplicationContext = getMuzimaApplicationContext();

        if (muzimaApplicationContext == null) {
            return;
        }

        SharedPreferences pref = muzimaApplicationContext.getSharedPreferences(Constants.SYNC_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        Date date = new Date();
        editor.putLong(AllCohortsListFragment.COHORTS_LAST_SYNCED_TIME, date.getTime());
        editor.commit();
        super.onPostExecute(result);
    }
}
