drop procedure if exists add_user_profile_column_if_missing;

delimiter //

create procedure add_user_profile_column_if_missing(
    in p_column_name varchar(64),
    in p_column_definition varchar(512)
)
begin
    if not exists (
        select 1
        from information_schema.columns
        where table_schema = database()
          and table_name = 'user_profile'
          and column_name = p_column_name
    ) then
        set @ddl = concat('alter table user_profile add column ', p_column_definition);
        prepare column_statement from @ddl;
        execute column_statement;
        deallocate prepare column_statement;
    end if;
end//

delimiter ;

call add_user_profile_column_if_missing('share_password_hash', 'share_password_hash varchar(100)');
call add_user_profile_column_if_missing('share_expires_at', 'share_expires_at datetime(6)');
call add_user_profile_column_if_missing('share_max_views', 'share_max_views int');
call add_user_profile_column_if_missing('share_view_count', 'share_view_count bigint not null default 0');
call add_user_profile_column_if_missing('share_last_viewed_at', 'share_last_viewed_at datetime(6)');

drop procedure if exists add_user_profile_column_if_missing;
