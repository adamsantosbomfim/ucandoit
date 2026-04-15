package com.example.fitnessapp.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ProfileDao_Impl implements ProfileDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ProfileEntity> __insertionAdapterOfProfileEntity;

  public ProfileDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfProfileEntity = new EntityInsertionAdapter<ProfileEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `profiles` (`userId`,`age`,`goal`,`heightCm`,`weightKg`,`notificationsEnabled`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ProfileEntity entity) {
        statement.bindLong(1, entity.getUserId());
        statement.bindLong(2, entity.getAge());
        if (entity.getGoal() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getGoal());
        }
        statement.bindLong(4, entity.getHeightCm());
        statement.bindDouble(5, entity.getWeightKg());
        final int _tmp = entity.getNotificationsEnabled() ? 1 : 0;
        statement.bindLong(6, _tmp);
      }
    };
  }

  @Override
  public Object upsert(final ProfileEntity profile, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfProfileEntity.insert(profile);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<ProfileEntity> observe(final long userId) {
    final String _sql = "SELECT * FROM profiles WHERE userId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, userId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"profiles"}, new Callable<ProfileEntity>() {
      @Override
      @Nullable
      public ProfileEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfAge = CursorUtil.getColumnIndexOrThrow(_cursor, "age");
          final int _cursorIndexOfGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "goal");
          final int _cursorIndexOfHeightCm = CursorUtil.getColumnIndexOrThrow(_cursor, "heightCm");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weightKg");
          final int _cursorIndexOfNotificationsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "notificationsEnabled");
          final ProfileEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpUserId;
            _tmpUserId = _cursor.getLong(_cursorIndexOfUserId);
            final int _tmpAge;
            _tmpAge = _cursor.getInt(_cursorIndexOfAge);
            final String _tmpGoal;
            if (_cursor.isNull(_cursorIndexOfGoal)) {
              _tmpGoal = null;
            } else {
              _tmpGoal = _cursor.getString(_cursorIndexOfGoal);
            }
            final int _tmpHeightCm;
            _tmpHeightCm = _cursor.getInt(_cursorIndexOfHeightCm);
            final float _tmpWeightKg;
            _tmpWeightKg = _cursor.getFloat(_cursorIndexOfWeightKg);
            final boolean _tmpNotificationsEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfNotificationsEnabled);
            _tmpNotificationsEnabled = _tmp != 0;
            _result = new ProfileEntity(_tmpUserId,_tmpAge,_tmpGoal,_tmpHeightCm,_tmpWeightKg,_tmpNotificationsEnabled);
          } else {
            _result = null;
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

  @Override
  public Object get(final long userId, final Continuation<? super ProfileEntity> $completion) {
    final String _sql = "SELECT * FROM profiles WHERE userId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, userId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ProfileEntity>() {
      @Override
      @Nullable
      public ProfileEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfAge = CursorUtil.getColumnIndexOrThrow(_cursor, "age");
          final int _cursorIndexOfGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "goal");
          final int _cursorIndexOfHeightCm = CursorUtil.getColumnIndexOrThrow(_cursor, "heightCm");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weightKg");
          final int _cursorIndexOfNotificationsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "notificationsEnabled");
          final ProfileEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpUserId;
            _tmpUserId = _cursor.getLong(_cursorIndexOfUserId);
            final int _tmpAge;
            _tmpAge = _cursor.getInt(_cursorIndexOfAge);
            final String _tmpGoal;
            if (_cursor.isNull(_cursorIndexOfGoal)) {
              _tmpGoal = null;
            } else {
              _tmpGoal = _cursor.getString(_cursorIndexOfGoal);
            }
            final int _tmpHeightCm;
            _tmpHeightCm = _cursor.getInt(_cursorIndexOfHeightCm);
            final float _tmpWeightKg;
            _tmpWeightKg = _cursor.getFloat(_cursorIndexOfWeightKg);
            final boolean _tmpNotificationsEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfNotificationsEnabled);
            _tmpNotificationsEnabled = _tmp != 0;
            _result = new ProfileEntity(_tmpUserId,_tmpAge,_tmpGoal,_tmpHeightCm,_tmpWeightKg,_tmpNotificationsEnabled);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
