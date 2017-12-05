
echo %date% %time%
sqlite3.exe majiang.db < majiang_jian.sql
echo %date% %time%
sqlite3.exe majiang.db < majiang_feng.sql
echo %date% %time%
sqlite3.exe majiang.db < majiang.sql
echo %date% %time%
pause