export default function SkeletonSummary() {
  return (
    <div className="bg-slate-800 p-6 rounded-xl border border-slate-700 animate-pulse">
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="h-16 bg-slate-700/40 rounded" />
        <div className="h-16 bg-slate-700/40 rounded" />
        <div className="h-16 bg-slate-700/40 rounded" />
      </div>
    </div>
  );
}
