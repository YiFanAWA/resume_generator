create table `user` (
    id int not null auto_increment,
    active bit not null,
    password varchar(255) not null,
    roles varchar(255) not null,
    user_name varchar(100) not null,
    primary key (id)
) engine=InnoDB;

create table user_profile (
    id int not null auto_increment,
    designation varchar(255),
    email varchar(255),
    first_name varchar(255),
    is_public bit not null,
    last_name varchar(255),
    phone varchar(255),
    share_token varchar(64),
    summary varchar(255),
    theme int not null,
    user_name varchar(100) not null,
    primary key (id)
) engine=InnoDB;

create table job (
    id int not null auto_increment,
    company varchar(255),
    designation varchar(255),
    end_date date,
    is_current_job bit not null,
    start_date date,
    user_profile_id int,
    primary key (id)
) engine=InnoDB;

create table education (
    id int not null auto_increment,
    college varchar(255),
    end_date date,
    qualification varchar(255),
    start_date date,
    summary varchar(255),
    user_profile_id int,
    primary key (id)
) engine=InnoDB;

create table user_profile_skills (
    user_profile_id int not null,
    skills varchar(255)
) engine=InnoDB;

create table job_responsibilities (
    job_id int not null,
    responsibilities varchar(255)
) engine=InnoDB;

alter table `user`
    add constraint uk_user_user_name unique (user_name);

alter table user_profile
    add constraint uk_user_profile_user_name unique (user_name);

alter table user_profile
    add constraint uk_user_profile_share_token unique (share_token);

create index idx_job_user_profile_id on job (user_profile_id);
create index idx_education_user_profile_id on education (user_profile_id);
create index idx_user_profile_skills_user_profile_id on user_profile_skills (user_profile_id);
create index idx_job_responsibilities_job_id on job_responsibilities (job_id);

alter table job
    add constraint fk_job_user_profile
    foreign key (user_profile_id) references user_profile (id);

alter table education
    add constraint fk_education_user_profile
    foreign key (user_profile_id) references user_profile (id);

alter table user_profile_skills
    add constraint fk_user_profile_skills_user_profile
    foreign key (user_profile_id) references user_profile (id);

alter table job_responsibilities
    add constraint fk_job_responsibilities_job
    foreign key (job_id) references job (id);
