package com.example.fitnessapp.data;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class WorkoutDao_Impl implements WorkoutDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<WorkoutEntity> __insertionAdapterOfWorkoutEntity;

  public WorkoutDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWorkoutEntity = new EntityInsertionAdapter<WorkoutEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `workouts` (`id`,`userId`,`title`,`durationMin`,`caloriesBurned`,`dateTime`,`createdAtEpochMs`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WorkoutEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getUserId());
        if (entity.getTitle() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getTitle());
        }
        statement.bindLong(4, entity.getDurationMin());
        statement.bindLong(5, entity.getCaloriesBurned());
        if (entity.getDateTime() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getDateTime());
        }
        statement.bindLong(7, entity.getCreatedAtEpochMs());
      }
    };
  }

  @Override
  public Object insert(final WorkoutEntity workout, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfWorkoutEntity.insertAndReturnId(workout);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<WorkoutEntity>> observeByUser(final long userId) {
    final String _sql = "SELECT * FROM workouts WHERE userId = ? ORDER BY createdAtEpochMs DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, userId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"workouts"}, new Callable<List<WorkoutEntity>>() {
      @Override
      @NonNull
      public List<WorkoutEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDurationMin = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMin");
          final int _cursorIndexOfCaloriesBurned = CursorUtil.getColumnIndexOrThrow(_cursor, "caloriesBurned");
          final int _cursorIndexOfDateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "dateTime");
          final int _cursorIndexOfCreatedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAtEpochMs");
          final List<WorkoutEntity> _result = new ArrayList<WorkoutEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorkoutEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpUserId;
            _tmpUserId = _cursor.getLong(_cursorIndexOfUserId);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final int _tmpDurationMin;
            _tmpDurationMin = _cursor.getInt(_cursorIndexOfDurationMin);
            final int _tmpCaloriesBurned;
            _tmpCaloriesBurned = _cursor.getInt(_cursorIndexOfCaloriesBurned);
            final String _tmpDateTime;
            if (_cursor.isNull(_cursorIndexOfDateTime)) {
              _tmpDateTime = null;
            } else {
              _tmpDateTime = _cursor.getString(_cursorIndexOfDateTime);
            }
            final long _tmpCreatedAtEpochMs;
            _tmpCreatedAtEpochMs = _cursor.getLong(_cursorIndexOfCreatedAtEpochMs);
            _item = new WorkoutEntity(_tmpId,_tmpUserId,_tmpTitle,_tmpDurationMin,_tmpCaloriesBurned,_tmpDateTime,_tmpCreatedAtEpochMs);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
