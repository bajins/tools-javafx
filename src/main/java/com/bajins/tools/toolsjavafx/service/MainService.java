package com.bajins.tools.toolsjavafx.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.ds.simple.SimpleDataSource;
import com.bajins.tools.toolsjavafx.model.RawData;
import com.bajins.tools.toolsjavafx.utils.JdbcUtil;
import jakarta.inject.Singleton;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * @author bajin
 */
@Singleton
public class MainService {

    private final static String MAIN_SQL = """
            -- 查询所有人员的投入项目工时，考虑实际情况，可能开发一部分然后会把需求转给其他人，使用pm_hours_log查实际投入
            with top as (
                select phl.pm_task_code, phl.user_code, pnp.pm_project_code, phl.pm_calculate_hours
                from pm_hours_log phl
                join pm_emp emp on phl.user_code=emp.user_code and emp.pm_ps_type=1 --and emp.pm_arrange_user='PG2006471'
                --		and phl.created_date >= DATE_TRUNC('year', CURRENT_DATE) AND phl.created_date < DATE_TRUNC('year', CURRENT_DATE) + INTERVAL '1 year'
                --	and phl.created_date >= '2025-01-01 00:00:00' AND phl.created_date <= '2025-12-31 23:59:59'
                    and emp.pm_arrange_user in ('PG1605125','PG1508090','PG1706192','PG1505071','PG2006471')
                join pm_dev pd on pd.pm_develop_code=phl.pm_task_code
                join pm_needs_propose pnp on pnp.pm_needs_code=pd.pm_needs_code and pnp.pm_project_code not in ('PGKF2017','D00902')
                where 1=1
            --	and pnp.pm_project_code in ('')
            ),
            ph as (
                select top.pm_project_code, sum(top.pm_calculate_hours) pm_prj_hours
                from top
                group by top.pm_project_code
            ),
            uh as (
                select top.user_code, top.pm_project_code, sum(top.pm_calculate_hours) pm_dev_hours
                from top
                group by top.user_code, top.pm_project_code
            ),
            uph as (
                select uh.user_code, iu.user_name, uh.pm_project_code, uh.pm_dev_hours, ph.pm_prj_hours, pp.pm_region, pr.pm_region_name, pp.pm_project_name
                , iu.dept_code, dp.dept_name, dp.parent_dept_code, pdp.dept_name parent_dept_name
                from uh
                join ims_user iu on iu.user_code=uh.user_code
                join ph on ph.pm_project_code=uh.pm_project_code
                join pm_project pp on pp.pm_project_code=uh.pm_project_code
                left join pm_emp pe on pe.user_code=uh.user_code
                left join pm_region pr on pr.pm_region_code=pe.pm_region_code
                join ims_dept dp on dp.dept_code=iu.dept_code
                left join ims_dept pdp on pdp.dept_code=dp.parent_dept_code
            ),
            ld as (
                select
                    tpp.pm_project_code,
                    tpp.user_code,
                    tpp.pm_region_code,
                    peu.user_name,
                    pr.pm_region_name
                    , peu.dept_code, dp.dept_name, dp.parent_dept_code, pdp.dept_name parent_dept_name
                from (
                    select
                        ppe.pm_project_code,
                        ppe.pm_transfer_in_date,
                        ppe.created_date,
                        pe.user_code,
                        pe.pm_region_code,
                        ROW_NUMBER() OVER (PARTITION BY ppe.pm_project_code ORDER BY ppe.pm_transfer_in_date DESC, ppe.created_date DESC) as rn
                    from pm_prj_emp ppe
                    join top on ppe.pm_project_code=top.pm_project_code and ppe.pm_is_lead_developer='y'
                    -- 考虑不同的部门
                    join pm_emp pe on pe.user_code=ppe.user_code --and pe.pm_region_code<>'05'
                        and pe.pm_arrange_user in ('PG1605125','PG1508090','PG1706192','PG1505071','PG2006471')
                ) tpp
                join ims_user peu on tpp.rn=1 and peu.user_code=tpp.user_code
                join pm_region pr on pr.pm_region_code=tpp.pm_region_code
                join ims_dept dp on dp.dept_code=peu.dept_code
                left join ims_dept pdp on pdp.dept_code=dp.parent_dept_code
            ),
            res as (
                select uph.user_code, uph.user_name, uph.pm_region_name as dev_region, uph.pm_project_code,
                    uph.dept_code, uph.dept_name, uph.parent_dept_code, uph.parent_dept_name,
                    CASE
                        WHEN uph.pm_project_name ~ '^[a-zA-Z]+$' THEN
                            -- 全是英文
                            uph.pm_project_name
                        WHEN uph.pm_project_name ~ '^[a-zA-Z]' THEN
                            -- 以英文开头
                            LEFT(uph.pm_project_name, 8)
                        ELSE
                            -- 包含中文或其他字符
                            LEFT(NULLIF(TRIM(uph.pm_project_name),''), 6)
                    END project_name,
                    case uph.pm_region
                        when 1 then '华南'
                        when 2 then '华东'
                        when 3 then '西南'
                        when 4 then '华北'
                        when 5 then '华中'
                        else uph.pm_region::numeric::TEXT
                    end pm_region,
                    ld.user_code as lead_user_code,
                    ld.user_name as lead_user_name,
                    ld.pm_region_name as lead_dev_region,
                    ld.dept_code  as lead_dept_code, ld.dept_name as lead_dept_name,
                    ld.parent_dept_code as lead_parent_dept_code, ld.parent_dept_name as lead_parent_dept_name,
                    uph.pm_dev_hours,
                    uph.pm_prj_hours
                from uph
                left join ld on ld.pm_project_code=uph.pm_project_code
            )
            select res.user_code, res.user_name, res.dev_region,
                res.dept_code, res.dept_name, res.parent_dept_code, res.parent_dept_name,
                res.pm_project_code, res.project_name,
                res.pm_region, res.lead_user_code, res.lead_user_name,
                case when res.lead_dev_region is null then res.pm_region else res.lead_dev_region end lead_dev_region,
                res.lead_dept_code, res.lead_dept_name, res.lead_parent_dept_code, res.lead_parent_dept_name,
                res.pm_dev_hours, res.pm_prj_hours
            from res
            """;

    // 查询排产人SQL
    private static final String QUERY_ARRANGE_USER_SQL = """
            SELECT iu.user_code, iu.user_name
            FROM ims_user iu
            WHERE iu.dept_code IN ('32023', '52', '1538', '1530', '58', '12523', '32022', '12521', '12021', '12522', '22521', '20521', '23021', '23022', '25021')
            AND iu.is_valid = 'y'
            AND EXISTS ( SELECT 1 FROM pm_emp pe WHERE pe.pm_arrange_user = iu.user_code AND pe.pm_ps_type = 1 AND pe.is_valid = 'y' )
            """;

    /**
     * 查询项目详情
     *
     * @param startDate    开始日期
     * @param endDate      结束日期
     * @param projectCodes 项目编码列表
     * @return 项目详情列表
     * @throws SQLException SQL异常
     */
    public List<Entity> queryProjectDetail(String startDate, String endDate, String projectCodes, String arrangeUsers) throws SQLException {
        String sql = MAIN_SQL;

        String dateReplaceKey = "--	and phl.created_date >= '2025-01-01 00:00:00' AND phl.created_date <= '2025-12-31 23:59:59'";
        boolean isBlankStartDate = StrUtil.isBlank(startDate);
        boolean isBlankEndDate = StrUtil.isBlank(endDate);
        if (!isBlankStartDate && !isBlankEndDate) {
            sql = sql.replace(dateReplaceKey, String.format(" and phl.created_date >= '%s 00:00:00' AND phl.created_date <= '%s 23:59:59'", startDate, endDate));
        } else if (!isBlankStartDate) {
            sql = sql.replace(dateReplaceKey, String.format(" and phl.created_date >= '%s 00:00:00'", startDate));
        } else if (!isBlankEndDate) {
            sql = sql.replace(dateReplaceKey, String.format(" and phl.created_date <= '%s 23:59:59'", endDate));
        }
        if (!StrUtil.isBlank(projectCodes)) {
            String projectCodesReplaceKey = "--\tand pnp.pm_project_code in ('')";
            sql = sql.replace(projectCodesReplaceKey, String.format(" and pnp.pm_project_code in %s", projectCodes));
        }
        if (!StrUtil.isBlank(arrangeUsers)) {
            String arrangeUsersReplaceKey = "and emp.pm_arrange_user in ('PG1605125','PG1508090','PG1706192','PG1505071','PG2006471')";
            sql = sql.replace(arrangeUsersReplaceKey, String.format(" and emp.pm_arrange_user in %s", arrangeUsers));
        }
        return JdbcUtil.query(sql);
    }

    /**
     * 查询项目详情
     *
     * @param startDate    开始日期
     * @param endDate      结束日期
     * @param projectCodes 项目编码列表
     * @param arrangeUsers    排产人编码列表
     * @return 项目详情列表
     * @throws SQLException SQL异常
     */
    public List<Entity> queryProjectDetail(LocalDate startDate, LocalDate endDate, String projectCodes, String arrangeUsers) throws SQLException {
        return queryProjectDetail(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE), endDate.format(DateTimeFormatter.ISO_LOCAL_DATE), projectCodes, arrangeUsers);
    }

    /**
     * 查询排产人
     *
     * @return 排产人列表
     * @throws SQLException SQL异常
     */
    public List<Entity> queryArrangeUser() throws SQLException {
        return JdbcUtil.query(QUERY_ARRANGE_USER_SQL);
    }
}
