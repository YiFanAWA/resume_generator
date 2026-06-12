alter table user_profile
    modify column summary text;

alter table education
    modify column summary text;

alter table user_profile_skills
    modify column skills text;

alter table job_responsibilities
    modify column responsibilities text;
