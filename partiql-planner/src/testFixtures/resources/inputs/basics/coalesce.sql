--#[coalesce-00]
-- type: (int32)
COALESCE(1);

--#[coalesce-01]
-- type: (int32)
COALESCE(1, 2);

--#[coalesce-02]
-- type: (decimal)
COALESCE(1, 1.23);

--#[coalesce-03]
-- type: (null | decimal)
COALESCE(NULL, 1, 1.23);

--#[coalesce-04]
-- type: (null | missing | decimal)
COALESCE(NULL, MISSING, 1, 1.23);

--#[coalesce-05]
-- type: (null | missing | decimal); same as above
COALESCE(1, 1.23, NULL, MISSING);

--#[coalesce-06]
-- type: (int32)
COALESCE(t_item.t_int32);

--#[coalesce-07]
-- type: (int32)
COALESCE(t_item.t_int32, t_item.t_int32);

--#[coalesce-08]
-- type: (int64)
COALESCE(t_item.t_int64, t_item.t_int32);

--#[coalesce-09]
-- type: (int64 | null)
COALESCE(t_item.t_int64_null, t_item.t_int32, t_item.t_int32_null);

--#[coalesce-10]
-- type: (int64 | null | missing)
COALESCE(t_item.t_int64_null, t_item.t_int32, t_item.t_int32_null, MISSING);

--#[coalesce-11]
-- type: (int64 | string)
COALESCE(t_item.t_int64, t_item.t_string);

--#[coalesce-12]
-- type: (int64 | null | string)
COALESCE(t_item.t_int64_null, t_item.t_string);

--#[coalesce-13]
-- type: (int16 | int32 | int64 | int | decimal)
COALESCE(t_item.t_num_exact, t_item.t_int32);

--#[coalesce-14]
-- type: (int16 | int32 | int64 | int | decimal, string)
COALESCE(t_item.t_num_exact, t_item.t_string);

--#[coalesce-15]
-- type: (int16 | int32 | int64 | int | decimal, string, null)
COALESCE(t_item.t_num_exact, t_item.t_string, NULL);

--#[coalesce-16]
-- type: (any)
COALESCE(t_item.t_any, t_item.t_int32);

--#[coalesce-17]
-- type: (any)
COALESCE(t_item.t_int32, t_item.t_any);